package com.pe.pivot.account.infrastructure.adapter.out.cache;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

/**
 * Decorator adding Reactive Cache-Aside via Redis on top of DebitCardPersistenceAdapter.
 */
@Slf4j
@Primary
@Component
public class CachedDebitCardRepositoryAdapter implements DebitCardRepositoryPort {
  // Cache key prefixes and TTL for Redis caching
  // Prefix for caching DebitCard by ID
  private static final String KEY_ID_PREFIX = "debitcard:id:";
  // Prefix for caching DebitCard by card number hash
  private static final String KEY_HASH_PREFIX = "debitcard:hash:";
  // Time-to-live for cached entries in Redis
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  private final DebitCardRepositoryPort delegate;
  private final ReactiveRedisTemplate<String, DebitCard> debitCardRedisTemplate;

  /**
   * Constructor for the CachedDebitCardRepositoryAdapter.
   * Qualifier is used to inject the specific delegate implementation
   * (DebitCardPersistenceAdapter) for persistence operations.
   *
   * @param delegate the underlying DebitCardRepositoryPort implementation for persistence
   *     operations
   * @param debitCardRedisTemplate the ReactiveRedisTemplate for caching DebitCard objects in
   *     Redis
   */
  public CachedDebitCardRepositoryAdapter(
          @Qualifier("debitCardPersistenceAdapter") DebitCardRepositoryPort delegate,
          ReactiveRedisTemplate<String, DebitCard> debitCardRedisTemplate) {
    this.delegate = delegate;
    this.debitCardRedisTemplate = debitCardRedisTemplate;
  }

  /**
   * Saves a DebitCard to the underlying repository and caches it in Redis.
   *
   * @param debitCard the DebitCard domain model to save
   * @return a Single emitting the saved DebitCard domain model
   */
  @Override
  public Single<DebitCard> save(DebitCard debitCard) {
    return delegate.save(debitCard).flatMap(this::cacheDebitCard);
  }

  /**
   * Retrieves all DebitCard records from the underlying repository.
   *
   * @return a Flowable emitting all DebitCard domain models
   */
  @Override
  public Flowable<DebitCard> findAll() {
    return delegate.findAll();
  }

  /**
   * Finds a DebitCard by its unique identifier, first checking the Redis cache before falling
   * back to the underlying repository.
   *
   * @param id the unique identifier of the DebitCard
   * @return a Maybe emitting the found DebitCard domain model or empty if not found
   */
  @Override
  public Maybe<DebitCard> findById(String id) {
    return getFromCache(KEY_ID_PREFIX + id)
            .switchIfEmpty(Maybe.defer(() -> delegate.findById(id)
                    .flatMap(card -> cacheDebitCard(card).toMaybe()))
            );
  }

  /**
   * Finds a DebitCard by its hashed card number, first checking the Redis cache before falling
   * back to the underlying repository.
   *
   * @param cardNumberHash the hashed card number of the DebitCard
   * @return a Maybe emitting the found DebitCard domain model or empty if not found
   */
  @Override
  public Maybe<DebitCard> findByCardNumberHash(String cardNumberHash) {
    return getFromCache(KEY_HASH_PREFIX + cardNumberHash)
            .switchIfEmpty(Maybe.defer(() -> delegate.findByCardNumberHash(cardNumberHash)
                    .flatMap(card -> cacheDebitCard(card).toMaybe()))
            );
  }

  /**
   * Finds all DebitCards associated with a given customerId.
   *
   * @param customerId the unique identifier of the customer
   * @return a Flowable emitting all DebitCard domain models for the specified customerId
   */
  @Override
  public Flowable<DebitCard> findByCustomerId(String customerId) {
    return delegate.findByCustomerId(customerId);
  }

  /**
   * Checks if a DebitCard exists with the specified hashed card number.
   *
   * @param cardNumberHash the hashed card number of the DebitCard
   * @return a Single emitting true if a DebitCard exists, false otherwise
   */
  @Override
  public Single<Boolean> existsByCardNumberHash(String cardNumberHash) {
    return delegate.existsByCardNumberHash(cardNumberHash);
  }

  /**
   * Deletes a DebitCard by its unique identifier, evicting it from the Redis cache after deletion.
   *
   * @param id the unique identifier of the DebitCard to delete
   * @return a Completable indicating completion of the delete operation
   */
  @Override
  public Completable deleteById(String id) {
    return delegate.deleteById(id)
            .andThen(evictFromCache(KEY_ID_PREFIX + id));
  }

  /**
   * Retrieves a DebitCard from the Redis cache by its cache key.
   *
   * @param key the cache key for the DebitCard in Redis
   * @return a Maybe emitting the found DebitCard or empty if not found in cache
   */
  private Maybe<DebitCard> getFromCache(String key) {
    return RxJava3Adapter.monoToMaybe(
            debitCardRedisTemplate.opsForValue().get(key)
                    .doOnNext(card -> log.debug("Redis cache HIT for key: {}", key))
                    .onErrorResume(ex -> {
                      log.warn("Redis read failed for key {}, falling back to MongoDB: {}",
                              key, ex.getMessage());
                      return Mono.empty();
                    })
    );
  }

  /**
   * Caches a DebitCard in Redis by both its ID and card number hash.
   *
   * @param card the DebitCard domain model to cache
   * @return a Single emitting the cached DebitCard
   */
  private Single<DebitCard> cacheDebitCard(DebitCard card) {
    // Cache the DebitCard in Redis by both its ID and card number hash, if they are not null
    Mono<Boolean> putById = card.getId() != null
            ? debitCardRedisTemplate.opsForValue()
            .set(KEY_ID_PREFIX + card.getId(), card, CACHE_TTL)
            : Mono.just(false);

    // Cache the DebitCard in Redis by its card number hash, if it is not null
    Mono<Boolean> putByHash = card.getCardNumberHash() != null
            ? debitCardRedisTemplate.opsForValue()
            .set(KEY_HASH_PREFIX + card.getCardNumberHash(), card, CACHE_TTL)
            : Mono.just(false);

    // Combine both caching operations and handle any errors gracefully, logging warnings if
    // Redis operations fail
    return RxJava3Adapter.monoToSingle(
            Mono.when(putById, putByHash)
                    .onErrorResume(ex -> {
                      log.warn(
                              "Redis write failed for debit card {}: {}",
                              card.getId(), ex.getMessage());
                      return Mono.empty();
                    })
                    .thenReturn(card)
    );
  }

  /**
   * Evicts a DebitCard from the Redis cache by its cache key.
   *
   * @param key the cache key for the DebitCard in Redis
   * @return a Completable indicating completion of the eviction operation
   */
  private Completable evictFromCache(String key) {
    return RxJava3Adapter.monoToCompletable(
            debitCardRedisTemplate.delete(key)
                    .onErrorResume(ex -> {
                      log.warn("Redis eviction failed for key {}: {}", key, ex.getMessage());
                      return Mono.just(0L);
                    })
                    .then()
    );
  }
}
