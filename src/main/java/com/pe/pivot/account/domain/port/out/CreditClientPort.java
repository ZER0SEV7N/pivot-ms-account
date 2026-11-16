package com.pe.pivot.account.domain.port.out;

import io.reactivex.rxjava3.core.Single;

/**
 * Output port for verifying whether a customer holds an active credit card in ms-credit (:8083).
 */
public interface CreditClientPort {

  Single<Boolean> hasActiveCreditCard(String customerId);
}
