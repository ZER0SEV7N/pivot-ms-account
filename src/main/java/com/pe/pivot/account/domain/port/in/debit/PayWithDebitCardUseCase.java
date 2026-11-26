package com.pe.pivot.account.domain.port.in.debit;

import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

/**
 * Input Port for processing payments with a debit card.
 */
public interface PayWithDebitCardUseCase {
  /**
   * Processes a payment using the specified debit card.
   *
   * @param debitCardId the unique identifier of the debit card
   * @param amount      the amount to be paid
   * @return Single emitting the result of the payment operation
   */
  Single<DebitCardPaymentResult> pay(String debitCardId, BigDecimal amount);
}
