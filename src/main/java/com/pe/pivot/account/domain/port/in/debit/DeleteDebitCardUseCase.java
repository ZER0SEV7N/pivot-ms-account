package com.pe.pivot.account.domain.port.in.debit;

import io.reactivex.rxjava3.core.Completable;

/**
 * Input Port for deleting a debit card by its unique identifier.
 */
public interface DeleteDebitCardUseCase {

  /**
   * Deletes a DebitCard by its unique identifier.
   *
   * @param id the unique identifier of the debit card to delete
   * @return Completable that completes if the deletion is successful, or emits an error if the
   *     debit card is not found or cannot be deleted
   */
  Completable delete(String id);
}
