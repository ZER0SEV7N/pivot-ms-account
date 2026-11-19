package com.pe.pivot.account.domain.port.in.debit;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for retrieving a debit card.
 */
public interface RetrieveDebitCardUseCase {

  /**
   * Retrieves all debit cards in the system.
   *
   * @return Flowable emitting all DebitCards
   */
  Flowable<DebitCard> findAll();

  /**
   * Retrieves a debit card by its unique identifier.
   *
   * @param id the unique identifier of the debit card
   * @return Single emitting the DebitCard if found, or an error if not found
   */
  Single<DebitCard> findById(String id);

  /**
   * Retrieves a debit card by its card number.
   *
   * @param cardNumber the unique card number of the debit card
   * @return Single emitting the DebitCard if found, or an error if not found
   */
  Single<DebitCard> findByCardNumber(String cardNumber);

  /**
   * Retrieves all debit cards associated with a given customer ID.
   *
   * @param customerId the unique identifier of the customer
   * @return Flowable emitting all DebitCards associated with the customer
   */
  Flowable<DebitCard> findByCustomerId(String customerId);

  /**
   * Retrieves the primary account balance associated with a given debit card ID.
   *
   * @param debitCardId the unique identifier of the debit card
   * @return Single emitting the primary account balance as a BankAccount, or an error if not found
   */
  Single<BankAccount> getPrimaryAccountBalance(String debitCardId);
}
