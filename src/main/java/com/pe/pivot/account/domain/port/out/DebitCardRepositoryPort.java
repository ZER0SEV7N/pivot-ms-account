package com.pe.pivot.account.domain.port.out;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Output port defining persistence operations for DebitCard domain models.
 */
public interface DebitCardRepositoryPort {

  /**
   * Saves a DebitCard to the repository.
   *
   * @param debitCard the DebitCard domain model to save
   * @return Single emitting the saved DebitCard
   */
  Single<DebitCard> save(DebitCard debitCard);

  /**
   * Retrieves all DebitCards from the repository.
   *
   * @return Flowable emitting all DebitCards
   */
  Flowable<DebitCard> findAll();

  /**
   * Finds a DebitCard by its unique identifier.
   *
   * @param id the unique identifier of the DebitCard
   * @return Maybe emitting the found DebitCard, or empty if not found
   */
  Maybe<DebitCard> findById(String id);

  /**
   * Finds a DebitCard by its hashed card number.
   *
   * @param cardNumberHash the hashed card number of the DebitCard
   * @return Maybe emitting the found DebitCard, or empty if not found
   */
  Maybe<DebitCard> findByCardNumberHash(String cardNumberHash);

  /**
   * Finds all DebitCards associated with a specific customer by their unique identifier.
   *
   * @param customerId the unique identifier of the customer
   * @return Flowable emitting all DebitCards associated with the customer
   */
  Flowable<DebitCard> findByCustomerId(String customerId);

  /**
   * Checks if a DebitCard exists in the repository by its hashed card number.
   *
   * @param cardNumberHash the hashed card number of the DebitCard
   * @return Single emitting true if the DebitCard exists, false otherwise
   */
  Single<Boolean> existsByCardNumberHash(String cardNumberHash);

  /**
   * Deletes a DebitCard from the repository by its unique identifier.
   *
   * @param id the unique identifier of the DebitCard to delete
   * @return Completable completing if the deletion was successful, or emitting an error if not
   */
  Completable deleteById(String id);
}