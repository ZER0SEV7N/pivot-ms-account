package com.pe.pivot.account.domain.port.in.yanki;

import com.pe.pivot.account.domain.model.yanki.YankiPaymentCommand;
import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import io.reactivex.rxjava3.core.Single;

/**
 * Input port for processing Yanki payment requests against primary accounts of linked debit cards.
 */
public interface ProcessYankiPaymentUseCase {

  /**
   * Processes a Yanki payment request by validating the debit card and primary account, and executing the payment.
   *
   * @param command the YankiPaymentCommand containing the payment details
   * @return a Single emitting the YankiPaymentOutcome indicating the result of the payment processing
   */
  Single<YankiPaymentOutcome> process(YankiPaymentCommand command);
}