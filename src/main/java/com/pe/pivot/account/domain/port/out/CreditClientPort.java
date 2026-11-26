package com.pe.pivot.account.domain.port.out;

import io.reactivex.rxjava3.core.Single;

/**
 * Output port for verifying customer credit card eligibility and overdue debt in ms-credit (:8083).
 */
public interface CreditClientPort {

  /**
   * Verifies whether a customer holds an active credit card.
   *
   * @param customerId the unique identifier of the customer
   * @return Single emitting true if the customer owns at least one active credit card
   */
  Single<Boolean> hasActiveCreditCard(String customerId);

  /**
   * Verifies whether a customer holds any unpaid overdue credit debt.
   *
   * @param customerId the unique identifier of the customer
   * @return Single emitting true if the customer has at least one overdue credit with debt
   */
  Single<Boolean> hasOverdueDebt(String customerId);
}