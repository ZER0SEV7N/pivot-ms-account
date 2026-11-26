package com.pe.pivot.account.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.model.yanki.YankiPaymentCommand;
import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import com.pe.pivot.account.domain.port.in.yanki.ProcessYankiPaymentUseCase;
import com.pe.pivot.account.domain.port.out.YankiEventPublisherPort;
import com.pe.pivot.account.infrastructure.adapter.in.messaging.dto.YankiPaymentRequestedEvent;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link YankiKafkaConsumerAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to consume Yanki payment requests")
class YankiKafkaConsumerAdapterTest {

  private static final Instant REQUESTED_AT = Instant.parse("2026-01-15T10:00:00Z");

  @Mock
  private ProcessYankiPaymentUseCase processYankiPaymentUseCase;

  @Mock
  private YankiEventPublisherPort eventPublisherPort;

  private YankiKafkaConsumerAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new YankiKafkaConsumerAdapter(processYankiPaymentUseCase, eventPublisherPort);
  }

  @Test
  @DisplayName("Should be processed and published when every mapped field is valid")
  void shouldBeProcessedAndPublishedWhenEveryMappedFieldIsValid() {
    // (happy path)
    // Arrange
    YankiPaymentRequestedEvent event = event();
    YankiPaymentOutcome outcome = YankiPaymentOutcome.builder()
            .transactionId("tx-1")
            .status("COMPLETED")
            .build();
    when(processYankiPaymentUseCase.process(any())).thenReturn(Single.just(outcome));
    when(eventPublisherPort.publishPaymentResult(outcome)).thenReturn(Completable.complete());

    // Act
    adapter.onYankiPaymentRequested(event);

    // Assert
    ArgumentCaptor<YankiPaymentCommand> captor = ArgumentCaptor.forClass(
            YankiPaymentCommand.class);
    verify(processYankiPaymentUseCase).process(captor.capture());
    YankiPaymentCommand command = captor.getValue();
    assertThat(command.transactionId()).isEqualTo("tx-1");
    assertThat(command.senderPhoneNumber()).isEqualTo("987654321");
    assertThat(command.receiverPhoneNumber()).isEqualTo("999888777");
    assertThat(command.senderDebitCardId()).isEqualTo("dc-sender");
    assertThat(command.receiverDebitCardId()).isEqualTo("dc-receiver");
    assertThat(command.currency()).isEqualTo("PEN");
    assertThat(command.amount()).isEqualByComparingTo("30.00");
    assertThat(command.description()).isEqualTo("Yanki payment");
    assertThat(command.requestedAt()).isEqualTo(REQUESTED_AT);
    verify(eventPublisherPort).publishPaymentResult(outcome);
  }

  @Test
  @DisplayName("Should be swallowed when the payment processing fails")
  void shouldBeSwallowedWhenPaymentProcessingFails() {
    // (error: payment processing failure)
    // Arrange
    when(processYankiPaymentUseCase.process(any()))
            .thenReturn(Single.error(new IllegalStateException("processing failure")));

    // Act & Assert
    assertThatCode(() -> adapter.onYankiPaymentRequested(event()))
            .doesNotThrowAnyException();
    verify(eventPublisherPort, never()).publishPaymentResult(any());
  }

  @Test
  @DisplayName("Should be swallowed when publishing the result fails")
  void shouldBeSwallowedWhenPublishingResultFails() {
    // (error: Kafka publish failure)
    // Arrange
    YankiPaymentOutcome outcome = YankiPaymentOutcome.builder()
            .transactionId("tx-1")
            .status("COMPLETED")
            .build();
    when(processYankiPaymentUseCase.process(any())).thenReturn(Single.just(outcome));
    when(eventPublisherPort.publishPaymentResult(outcome))
            .thenReturn(Completable.error(new IllegalStateException("broker unavailable")));

    // Act & Assert
    assertThatCode(() -> adapter.onYankiPaymentRequested(event()))
            .doesNotThrowAnyException();
    verify(eventPublisherPort).publishPaymentResult(outcome);
  }

  private YankiPaymentRequestedEvent event() {
    return YankiPaymentRequestedEvent.builder()
            .transactionId("tx-1")
            .senderPhoneNumber("987654321")
            .receiverPhoneNumber("999888777")
            .senderDebitCardId("dc-sender")
            .receiverDebitCardId("dc-receiver")
            .currency("PEN")
            .amount(new BigDecimal("30.00"))
            .description("Yanki payment")
            .requestedAt(REQUESTED_AT)
            .build();
  }
}
