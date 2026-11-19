package com.pe.pivot.account.infrastructure.adapter.in.messaging;

import com.pe.pivot.account.domain.model.yanki.YankiPaymentCommand;
import com.pe.pivot.account.domain.port.in.yanki.ProcessYankiPaymentUseCase;
import com.pe.pivot.account.domain.port.out.YankiEventPublisherPort;
import com.pe.pivot.account.infrastructure.adapter.in.messaging.dto.YankiPaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer Adapter listening to yanki.payment.requested and triggering Yanki settlement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YankiKafkaConsumerAdapter {

  private final ProcessYankiPaymentUseCase processYankiPaymentUseCase;
  private final YankiEventPublisherPort eventPublisherPort;

  /**
   * Consumes Yanki payment requests, processes primary account movements, and emits the result.
   *
   * @param event the incoming YankiPaymentRequestedEvent from Kafka
   */
  @KafkaListener(
          topics = "${application.kafka-topics.yanki-payment-requested}",
          groupId = "${spring.kafka.consumer.group-id}")
  public void onYankiPaymentRequested(YankiPaymentRequestedEvent event) {
    log.info("Received YankiPaymentRequestedEvent for TxId: {}", event.transactionId());
    // Map the incoming event to a YankiPaymentCommand for processing
    YankiPaymentCommand command = YankiPaymentCommand.builder()
            .transactionId(event.transactionId())
            .senderPhoneNumber(event.senderPhoneNumber())
            .receiverPhoneNumber(event.receiverPhoneNumber())
            .senderDebitCardId(event.senderDebitCardId())
            .receiverDebitCardId(event.receiverDebitCardId())
            .currency(event.currency())
            .amount(event.amount())
            .description(event.description())
            .requestedAt(event.requestedAt())
            .build();

    // Process the Yanki payment command and publish the result, handling errors gracefully
    processYankiPaymentUseCase.process(command)
            .flatMapCompletable(eventPublisherPort::publishPaymentResult)
            .doOnComplete(() -> log.debug(
                    "Yanki Kafka workflow finished for TxId: {}", event.transactionId()))
            .doOnError(error -> log.error(
                    "Unexpected error publishing Yanki result for TxId {}: {}",
                    event.transactionId(), error.getMessage(), error))
            .onErrorComplete()
            .blockingAwait();
  }
}