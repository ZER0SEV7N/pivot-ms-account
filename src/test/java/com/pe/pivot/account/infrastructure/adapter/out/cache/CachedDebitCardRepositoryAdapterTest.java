package com.pe.pivot.account.infrastructure.adapter.out.cache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link CachedDebitCardRepositoryAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to cache debit cards through Redis")
class CachedDebitCardRepositoryAdapterTest {

  private static final String CARD_ID = "dc-1";
  private static final String CARD_HASH = "hash-value";
  private static final String OTHER_HASH = "other-hash";
  private static final String ID_KEY = "debitcard:id:" + CARD_ID;
  private static final String HASH_KEY = "debitcard:hash:" + CARD_HASH;
  private static final Duration TTL = Duration.ofMinutes(15);

  @Mock
  private DebitCardRepositoryPort delegate;

  @Mock
  private ReactiveRedisTemplate<String, DebitCard> redisTemplate;

  @Mock
  private ReactiveValueOperations<String, DebitCard> valueOperations;

  private CachedDebitCardRepositoryAdapter adapter;

  private DebitCard card;

  @BeforeEach
  void setUp() {
    adapter = new CachedDebitCardRepositoryAdapter(delegate, redisTemplate);
    card = DebitCard.builder().id(CARD_ID).cardNumberHash(CARD_HASH).build();
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  @DisplayName("Should be saved and cached under the id and hash keys")
  void shouldBeSavedAndCachedUnderIdAndHashKeys() {
    // (happy path)
    // Arrange
    when(delegate.save(card)).thenReturn(Single.just(card));
    when(valueOperations.set(ID_KEY, card, TTL)).thenReturn(Mono.just(true));
    when(valueOperations.set(HASH_KEY, card, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.save(card).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate).save(card);
    verify(valueOperations).set(ID_KEY, card, TTL);
    verify(valueOperations).set(HASH_KEY, card, TTL);
  }

  @Test
  @DisplayName("Should be cached only under the available key when fields are missing")
  void shouldBeCachedOnlyUnderAvailableKeyWhenFieldsAreMissing() {
    // Arrange
    DebitCard partial = DebitCard.builder().id(CARD_ID).build();
    when(delegate.save(partial)).thenReturn(Single.just(partial));
    when(valueOperations.set(ID_KEY, partial, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.save(partial).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(partial);
    verify(valueOperations).set(ID_KEY, partial, TTL);
    verify(valueOperations, never()).set(eq(HASH_KEY), any(DebitCard.class), eq(TTL));
  }

  @Test
  @DisplayName("Should be returned even when the cache write fails")
  void shouldBeReturnedEvenWhenCacheWriteFails() {
    // (error: Redis write failure, fail-open)
    // Arrange
    when(delegate.save(card)).thenReturn(Single.just(card));
    when(valueOperations.set(ID_KEY, card, TTL))
            .thenReturn(Mono.error(new IllegalStateException("redis down")));
    when(valueOperations.set(HASH_KEY, card, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.save(card).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
  }

  @Test
  @DisplayName("Should be propagated when the persistence save fails")
  void shouldBePropagatedWhenPersistenceSaveFails() {
    // (error: MongoDB save failure)
    // Arrange
    when(delegate.save(card))
            .thenReturn(Single.error(new IllegalStateException("mongo down")));

    // Act
    TestObserver<DebitCard> observer = adapter.save(card).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
    verify(valueOperations, never()).set(anyString(), any(DebitCard.class), eq(TTL));
  }

  @Test
  @DisplayName("Should be delegated when retrieving every debit card")
  void shouldBeDelegatedWhenRetrievingEveryDebitCard() {
    // Arrange
    DebitCard second = DebitCard.builder().id("dc-2").build();
    when(delegate.findAll()).thenReturn(Flowable.just(card, second));

    // Act
    TestSubscriber<DebitCard> observer = adapter.findAll().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(2);
    observer.assertValues(card, second);
    verify(valueOperations, never()).get(anyString());
  }

  @Test
  @DisplayName("Should be served from the cache when the id is present")
  void shouldBeServedFromCacheWhenIdIsPresent() {
    // Arrange
    when(valueOperations.get(ID_KEY)).thenReturn(Mono.just(card));

    // Act
    TestObserver<DebitCard> observer = adapter.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate, never()).findById(CARD_ID);
  }

  @Test
  @DisplayName("Should be read from the database and cached when the id is missing")
  void shouldBeReadFromDatabaseAndCachedWhenIdIsMissing() {
    // Arrange
    when(valueOperations.get(ID_KEY)).thenReturn(Mono.empty());
    when(delegate.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(valueOperations.set(ID_KEY, card, TTL)).thenReturn(Mono.just(true));
    when(valueOperations.set(HASH_KEY, card, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate).findById(CARD_ID);
    verify(valueOperations).set(ID_KEY, card, TTL);
  }

  @Test
  @DisplayName("Should be completed empty when the id is neither cached nor persisted")
  void shouldBeCompletedEmptyWhenIdIsNeitherCachedNorPersisted() {
    // (error: debit card not found)
    // Arrange
    when(valueOperations.get(ID_KEY)).thenReturn(Mono.empty());
    when(delegate.findById(CARD_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<DebitCard> observer = adapter.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should be read from the database when Redis fails on the id lookup")
  void shouldBeReadFromDatabaseWhenRedisFailsOnIdLookup() {
    // (error: Redis read failure, fail-open)
    // Arrange
    when(valueOperations.get(ID_KEY))
            .thenReturn(Mono.error(new IllegalStateException("redis down")));
    when(delegate.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(valueOperations.set(ID_KEY, card, TTL)).thenReturn(Mono.just(true));
    when(valueOperations.set(HASH_KEY, card, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate).findById(CARD_ID);
  }

  @Test
  @DisplayName("Should be served from the cache when the hash is present")
  void shouldBeServedFromCacheWhenHashIsPresent() {
    // Arrange
    when(valueOperations.get(HASH_KEY)).thenReturn(Mono.just(card));

    // Act
    TestObserver<DebitCard> observer = adapter.findByCardNumberHash(CARD_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate, never()).findByCardNumberHash(CARD_HASH);
  }

  @Test
  @DisplayName("Should be read from the database and cached when the hash is missing")
  void shouldBeReadFromDatabaseAndCachedWhenHashIsMissing() {
    // Arrange
    when(valueOperations.get(HASH_KEY)).thenReturn(Mono.empty());
    when(delegate.findByCardNumberHash(CARD_HASH)).thenReturn(Maybe.just(card));
    when(valueOperations.set(ID_KEY, card, TTL)).thenReturn(Mono.just(true));
    when(valueOperations.set(HASH_KEY, card, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.findByCardNumberHash(CARD_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate).findByCardNumberHash(CARD_HASH);
    verify(valueOperations).set(HASH_KEY, card, TTL);
  }

  @Test
  @DisplayName("Should be read from the database when Redis fails on the hash lookup")
  void shouldBeReadFromDatabaseWhenRedisFailsOnHashLookup() {
    // (error: Redis read failure, fail-open)
    // Arrange
    when(valueOperations.get(HASH_KEY))
            .thenReturn(Mono.error(new IllegalStateException("redis down")));
    when(delegate.findByCardNumberHash(CARD_HASH)).thenReturn(Maybe.just(card));
    when(valueOperations.set(ID_KEY, card, TTL)).thenReturn(Mono.just(true));
    when(valueOperations.set(HASH_KEY, card, TTL)).thenReturn(Mono.just(true));

    // Act
    TestObserver<DebitCard> observer = adapter.findByCardNumberHash(CARD_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(delegate).findByCardNumberHash(CARD_HASH);
  }

  @Test
  @DisplayName("Should be delegated when searching by customer id")
  void shouldBeDelegatedWhenSearchingByCustomerId() {
    // Arrange
    when(delegate.findByCustomerId("cust-1")).thenReturn(Flowable.just(card));

    // Act
    TestSubscriber<DebitCard> observer = adapter.findByCustomerId("cust-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(card);
    verify(delegate).findByCustomerId("cust-1");
  }

  @Test
  @DisplayName("Should be delegated when checking a card number hash")
  void shouldBeDelegatedWhenCheckingCardNumberHash() {
    // Arrange
    when(delegate.existsByCardNumberHash(OTHER_HASH)).thenReturn(Single.just(false));

    // Act
    TestObserver<Boolean> observer = adapter.existsByCardNumberHash(OTHER_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(false);
    verify(delegate).existsByCardNumberHash(OTHER_HASH);
  }

  @Test
  @DisplayName("Should be deleted and evicted from the cache")
  void shouldBeDeletedAndEvictedFromCache() {
    // Arrange
    when(delegate.deleteById(CARD_ID)).thenReturn(Completable.complete());
    when(redisTemplate.delete(ID_KEY)).thenReturn(Mono.just(1L));

    // Act
    TestObserver<Void> observer = adapter.deleteById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verify(delegate).deleteById(CARD_ID);
    verify(redisTemplate).delete(ID_KEY);
  }

  @Test
  @DisplayName("Should be completed even when the cache eviction fails")
  void shouldBeCompletedEvenWhenCacheEvictionFails() {
    // (error: Redis eviction failure, fail-open)
    // Arrange
    when(delegate.deleteById(CARD_ID)).thenReturn(Completable.complete());
    when(redisTemplate.delete(ID_KEY))
            .thenReturn(Mono.error(new IllegalStateException("redis down")));

    // Act
    TestObserver<Void> observer = adapter.deleteById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verify(redisTemplate).delete(ID_KEY);
  }

  @Test
  @DisplayName("Should be propagated when the persistence deletion fails")
  void shouldBePropagatedWhenPersistenceDeletionFails() {
    // (error: MongoDB deletion failure)
    // Arrange
    when(delegate.deleteById(CARD_ID))
            .thenReturn(Completable.error(new IllegalStateException("mongo down")));
    when(redisTemplate.delete(ID_KEY)).thenReturn(Mono.just(1L));

    // Act
    TestObserver<Void> observer = adapter.deleteById(CARD_ID).test();

    // Assert
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
  }
}
