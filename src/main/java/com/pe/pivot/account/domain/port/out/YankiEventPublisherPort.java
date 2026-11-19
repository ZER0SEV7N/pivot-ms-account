package com.pe.pivot.account.domain.port.out;

import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import io.reactivex.rxjava3.core.Completable;

/**
 * Output port for publishing Yanki payment outcomes to the event broker.
 */
public interface YankiEventPublisherPort {

  /**
   * Publishes the result of a Yanki payment operation to the event broker.
   *
   * @param outcome the YankiPaymentOutcome containing the details of the payment result
   * @return Completable that completes when the event is successfully published, or emits an error if the publishing fails
   */
  Completable publishPaymentResult(YankiPaymentOutcome outcome);
}