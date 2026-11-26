package com.pe.pivot.account.infrastructure.adapter.out.persistence.debit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.entity.DebitCardDocument;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.mapper.DebitCardPersistenceMapper;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.repository.ReactiveDebitCardMongoRepository;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link DebitCardPersistenceAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to persist debit cards in MongoDB")
class DebitCardPersistenceAdapterTest {

  private static final String CARD_ID = "dc-1";
  private static final String CUSTOMER_ID = "cust-1";
  private static final String CARD_HASH = "hash-value";

  @Mock
  private ReactiveDebitCardMongoRepository repository;

  @Mock
  private DebitCardPersistenceMapper mapper;

  private DebitCardPersistenceAdapter adapter;

  private DebitCard domain;
  private DebitCardDocument document;

  @BeforeEach
  void setUp() {
    adapter = new DebitCardPersistenceAdapter(repository, mapper);
    domain = DebitCard.builder().id(CARD_ID).customerId(CUSTOMER_ID).build();
    document = DebitCardDocument.builder().id(CARD_ID).customerId(CUSTOMER_ID).build();
  }

  @Test
  @DisplayName("Should be saved and mapped back when persisting a debit card")
  void shouldBeSavedAndMappedBackWhenPersistingDebitCard() {
    // (happy path)
    // Arrange
    when(mapper.toDocument(domain)).thenReturn(document);
    when(repository.save(document)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act
    TestObserver<DebitCard> observer = adapter.save(domain).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(domain);
    verify(repository).save(document);
  }

  @Test
  @DisplayName("Should be mapped when retrieving every debit card")
  void shouldBeMappedWhenRetrievingEveryDebitCard() {
    // Arrange
    DebitCard second = DebitCard.builder().id("dc-2").build();
    DebitCardDocument secondDocument = DebitCardDocument.builder().id("dc-2").build();
    when(repository.findAll()).thenReturn(Flux.just(document, secondDocument));
    when(mapper.toDomain(document)).thenReturn(domain);
    when(mapper.toDomain(secondDocument)).thenReturn(second);

    // Act
    TestSubscriber<DebitCard> observer = adapter.findAll().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(2);
    observer.assertValues(domain, second);
  }

  @Test
  @DisplayName("Should be found when the id exists")
  void shouldBeFoundWhenIdExists() {
    // Arrange
    when(repository.findById(CARD_ID)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act
    TestObserver<DebitCard> observer = adapter.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(domain);
  }

  @Test
  @DisplayName("Should be completed empty when the id does not exist")
  void shouldBeCompletedEmptyWhenIdDoesNotExist() {
    // (error: debit card not found)
    // Arrange
    when(repository.findById(CARD_ID)).thenReturn(Mono.empty());

    // Act
    TestObserver<DebitCard> observer = adapter.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should be found when the card number hash exists")
  void shouldBeFoundWhenCardNumberHashExists() {
    // Arrange
    when(repository.findByCardNumberHash(CARD_HASH)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act
    TestObserver<DebitCard> observer = adapter.findByCardNumberHash(CARD_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(domain);
  }

  @Test
  @DisplayName("Should be mapped when searching by customer id")
  void shouldBeMappedWhenSearchingByCustomerId() {
    // Arrange
    when(repository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flux.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act
    TestSubscriber<DebitCard> observer = adapter.findByCustomerId(CUSTOMER_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(domain);
  }

  @Test
  @DisplayName("Should be reported when the card number hash exists")
  void shouldBeReportedWhenCardNumberHashExists() {
    // Arrange
    when(repository.existsByCardNumberHash(CARD_HASH)).thenReturn(Mono.just(true));

    // Act
    TestObserver<Boolean> observer = adapter.existsByCardNumberHash(CARD_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(true);
  }

  @Test
  @DisplayName("Should be completed when deleting a debit card")
  void shouldBeCompletedWhenDeletingDebitCard() {
    // Arrange
    when(repository.deleteById(CARD_ID)).thenReturn(Mono.empty());

    // Act
    TestObserver<Void> observer = adapter.deleteById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertNoValues();
    verify(repository).deleteById(CARD_ID);
  }
}
