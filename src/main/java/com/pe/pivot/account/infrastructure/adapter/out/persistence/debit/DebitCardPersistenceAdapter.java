package com.pe.pivot.account.infrastructure.adapter.out.persistence.debit;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.mapper.DebitCardPersistenceMapper;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.repository.ReactiveDebitCardMongoRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * MongoDB Persistence Adapter implementing DebitCardRepositoryPort using RxJava 3.
 */
@Component
@RequiredArgsConstructor
public class DebitCardPersistenceAdapter implements DebitCardRepositoryPort {

  private final ReactiveDebitCardMongoRepository repository;
  private final DebitCardPersistenceMapper mapper;

  /**
   * Saves a DebitCard domain model to the MongoDB database.
   *
   * @param debitCard the DebitCard domain model to save
   * @return a Single emitting the saved DebitCard domain model
   */
  @Override
  public Single<DebitCard> save(DebitCard debitCard) {
    return RxJava3Adapter.monoToSingle(
            repository.save(mapper.toDocument(debitCard)).map(mapper::toDomain));
  }

  /**
   * Retrieves all DebitCard records from the MongoDB database.
   *
   * @return a Flowable emitting all DebitCard domain models
   */
  @Override
  public Flowable<DebitCard> findAll() {
    return RxJava3Adapter.fluxToFlowable(repository.findAll().map(mapper::toDomain));
  }

  /**
   * Finds a DebitCard by its unique identifier.
   *
   * @param id the unique identifier of the DebitCard
   * @return a Maybe emitting the found DebitCard domain model or empty if not found
   */
  @Override
  public Maybe<DebitCard> findById(String id) {
    return RxJava3Adapter.monoToMaybe(repository.findById(id).map(mapper::toDomain));
  }

  /**
   * Finds a DebitCard by its hashed card number.
   *
   * @param cardNumberHash the hashed card number of the DebitCard
   * @return a Maybe emitting the found DebitCard domain model or empty if not found
   */
  @Override
  public Maybe<DebitCard> findByCardNumberHash(String cardNumberHash) {
    return RxJava3Adapter.monoToMaybe(
            repository.findByCardNumberHash(cardNumberHash).map(mapper::toDomain));
  }

  /**
   * Finds all DebitCards associated with a given customerId.
   *
   * @param customerId the unique identifier of the customer
   * @return a Flowable emitting all DebitCard domain models for the specified customerId
   */
  @Override
  public Flowable<DebitCard> findByCustomerId(String customerId) {
    return RxJava3Adapter.fluxToFlowable(
            repository.findByCustomerId(customerId).map(mapper::toDomain));
  }

  /**
   * Checks if a DebitCard exists with the specified hashed card number.
   *
   * @param cardNumberHash the hashed card number of the DebitCard
   * @return a Single emitting true if a DebitCard exists with the given cardNumberHash, false otherwise
   */
  @Override
  public Single<Boolean> existsByCardNumberHash(String cardNumberHash) {
    return RxJava3Adapter.monoToSingle(repository.existsByCardNumberHash(cardNumberHash));
  }

  /**
   * Deletes a DebitCard by its unique identifier.
   *
   * @param id the unique identifier of the DebitCard to delete
   * @return a Completable that completes if the deletion is successful,
   *         or emits an error if the DebitCard does not exist
   */
  @Override
  public Completable deleteById(String id) {
    return RxJava3Adapter.monoToCompletable(repository.deleteById(id));
  }
}
