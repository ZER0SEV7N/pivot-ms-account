package com.pe.pivot.account.infrastructure.adapter.in.messaging;

import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import com.pe.pivot.account.domain.port.out.YankiEventPublisherPort;
import com.pe.pivot.account.infrastructure.adapter.out.messaging.dto.YankiPaymentResultEvent;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Producer Adapter publishing Yanki payment outcomes to yanki.payment.result.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YankiKafkaProducerAdapter implements YankiEventPublisherPort {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${application.kafka-topics.yanki-payment-result}")
  private String paymentResultTopic;

  /**
   * Publishes a Yanki payment result event to the Kafka topic.
   *
   * @param outcome the YankiPaymentOutcome containing the details of the payment result
   * @return Completable that completes when the event is successfully published to Kafka, or emits an error if the publishing fails
   */
  @Override
  public Completable publishPaymentResult(YankiPaymentOutcome outcome) {
    YankiPaymentResultEvent event = YankiPaymentResultEvent.builder()
            .transactionId(outcome.transactionId())
            .status(outcome.status())
            .senderPhoneNumber(outcome.senderPhoneNumber())
            .receiverPhoneNumber(outcome.receiverPhoneNumber())
            .senderDebitCardId(outcome.senderDebitCardId())
            .receiverDebitCardId(outcome.receiverDebitCardId())
            .debitedAccountId(outcome.debitedAccountId())
            .creditedAccountId(outcome.creditedAccountId())
            .currency(outcome.currency())
            .amount(outcome.amount())
            .failureReason(outcome.failureReason())
            .processedAt(outcome.processedAt())
            .build();

    // Publish the event to the configured Kafka topic and log the outcome
    return Completable.fromCompletionStage(
                    kafkaTemplate.send(paymentResultTopic, outcome.transactionId(), event))
            .doOnComplete(() -> log.info(
                    "Published YankiPaymentResultEvent to topic {} for TxId: {}, Status: {}",
                    paymentResultTopic, outcome.transactionId(), outcome.status()));
  }
}