package com.pe.pivot.account.domain.model.yanki;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Yanki payment domain records.
 */
@DisplayName("Should be able to carry Yanki payment data")
class YankiPaymentModelsTest {

  private static final Instant REQUESTED_AT = Instant.parse("2026-01-15T10:00:00Z");

  @Test
  @DisplayName("Should be built with the full command data")
  void shouldBeBuiltWithFullCommandData() {
    // (happy path)
    // Arrange / Act
    YankiPaymentCommand command = YankiPaymentCommand.builder()
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

    // Assert
    assertThat(command.transactionId()).isEqualTo("tx-1");
    assertThat(command.senderPhoneNumber()).isEqualTo("987654321");
    assertThat(command.receiverPhoneNumber()).isEqualTo("999888777");
    assertThat(command.senderDebitCardId()).isEqualTo("dc-sender");
    assertThat(command.receiverDebitCardId()).isEqualTo("dc-receiver");
    assertThat(command.currency()).isEqualTo("PEN");
    assertThat(command.amount()).isEqualByComparingTo("30.00");
    assertThat(command.description()).isEqualTo("Yanki payment");
    assertThat(command.requestedAt()).isEqualTo(REQUESTED_AT);
  }

  @Test
  @DisplayName("Should be built with the full outcome data")
  void shouldBeBuiltWithFullOutcomeData() {
    // Arrange / Act
    YankiPaymentOutcome outcome = YankiPaymentOutcome.builder()
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
            .failureReason(null)
            .processedAt(REQUESTED_AT)
            .build();

    // Assert
    assertThat(outcome.transactionId()).isEqualTo("tx-1");
    assertThat(outcome.status()).isEqualTo("COMPLETED");
    assertThat(outcome.senderPhoneNumber()).isEqualTo("987654321");
    assertThat(outcome.receiverPhoneNumber()).isEqualTo("999888777");
    assertThat(outcome.senderDebitCardId()).isEqualTo("dc-sender");
    assertThat(outcome.receiverDebitCardId()).isEqualTo("dc-receiver");
    assertThat(outcome.debitedAccountId()).isEqualTo("acc-sender");
    assertThat(outcome.creditedAccountId()).isEqualTo("acc-receiver");
    assertThat(outcome.currency()).isEqualTo("PEN");
    assertThat(outcome.amount()).isEqualByComparingTo("30.00");
    assertThat(outcome.failureReason()).isNull();
    assertThat(outcome.processedAt()).isEqualTo(REQUESTED_AT);
  }
}
