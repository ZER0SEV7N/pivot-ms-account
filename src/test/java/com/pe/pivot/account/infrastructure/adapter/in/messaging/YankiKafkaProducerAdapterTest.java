package com.pe.pivot.account.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import com.pe.pivot.account.infrastructure.adapter.out.messaging.dto.YankiPaymentResultEvent;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link YankiKafkaProducerAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to publish Yanki payment results")
class YankiKafkaProducerAdapterTest {

  private static final String TOPIC = "yanki.payment.result";
  private static final Instant PROCESSED_AT = Instant.parse("2026-01-15T10:00:00Z");

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  private YankiKafkaProducerAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new YankiKafkaProducerAdapter(kafkaTemplate);
    ReflectionTestUtils.setField(adapter, "paymentResultTopic", TOPIC);
  }

  @Test
  @DisplayName("Should be published with every mapped field when Kafka accepts the event")
  void shouldBePublishedWithEveryMappedFieldWhenKafkaAcceptsEvent() {
    // (happy path)
    // Arrange
    YankiPaymentOutcome outcome = outcome();
    when(kafkaTemplate.send(eq(TOPIC), eq("tx-1"), any(YankiPaymentResultEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

    // Act
    TestObserver<Void> observer = adapter.publishPaymentResult(outcome).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(kafkaTemplate).send(eq(TOPIC), eq("tx-1"), captor.capture());
    YankiPaymentResultEvent event = (YankiPaymentResultEvent) captor.getValue();
    assertThat(event.transactionId()).isEqualTo("tx-1");
    assertThat(event.status()).isEqualTo("COMPLETED");
    assertThat(event.senderPhoneNumber()).isEqualTo("987654321");
    assertThat(event.receiverPhoneNumber()).isEqualTo("999888777");
    assertThat(event.senderDebitCardId()).isEqualTo("dc-sender");
    assertThat(event.receiverDebitCardId()).isEqualTo("dc-receiver");
    assertThat(event.debitedAccountId()).isEqualTo("acc-sender");
    assertThat(event.creditedAccountId()).isEqualTo("acc-receiver");
    assertThat(event.currency()).isEqualTo("PEN");
    assertThat(event.amount()).isEqualByComparingTo("30.00");
    assertThat(event.failureReason()).isNull();
    assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
  }

  @Test
  @DisplayName("Should be propagated when Kafka rejects the event")
  void shouldBePropagatedWhenKafkaRejectsEvent() {
    // (error: Kafka publish failure)
    // Arrange
    CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
    failed.completeExceptionally(new IllegalStateException("broker unavailable"));
    when(kafkaTemplate.send(eq(TOPIC), eq("tx-1"), any(YankiPaymentResultEvent.class)))
            .thenReturn(failed);

    // Act
    TestObserver<Void> observer = adapter.publishPaymentResult(outcome()).test();

    // Assert
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
  }

  private YankiPaymentOutcome outcome() {
    return YankiPaymentOutcome.builder()
            .transactionId("tx-1")
            .status("COMPLETED")
            .senderPhoneNumber("987654321")
            .receiverPhoneNumber("999888777")
            .senderDebitCardId("dc-sender")
            .receiverDebitCardId("dc-receiver")
            .debitedAccountId("acc-sender")
            .creditedAccountId("acc-receiver")
            .currency("PEN")
            .amount(new BigDecimal("30.00"))
            .processedAt(PROCESSED_AT)
            .build();
  }
}
