package com.pe.pivot.account.domain.model.debit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebitCardPaymentResult} and {@link DebitedAccountEntry}.
 */
@DisplayName("Should be able to expose debit card payment results")
class DebitCardPaymentResultTest {

  private static final Instant PROCESSED_AT = Instant.parse("2026-01-15T10:00:00Z");

  @Test
  @DisplayName("Should be built with the full payment breakdown")
  void shouldBeBuiltWithFullPaymentBreakdown() {
    // (happy path)
    // Arrange
    DebitedAccountEntry entry = DebitedAccountEntry.builder()
            .accountId("acc-1")
            .maskedAccountNumber("****5678")
            .debitedAmount(new BigDecimal("100.00"))
            .appliedCommission(new BigDecimal("1.50"))
            .previousBalance(new BigDecimal("500.00"))
            .newBalance(new BigDecimal("400.00"))
            .build();

    // Act
    DebitCardPaymentResult result = DebitCardPaymentResult.builder()
            .debitCardId("dc-1")
            .maskedCardNumber("****1234")
            .totalAmount(new BigDecimal("100.00"))
            .totalCommission(new BigDecimal("1.50"))
            .debitedAccounts(List.of(entry))
            .processedAt(PROCESSED_AT)
            .build();

    // Assert
    assertThat(result.debitCardId()).isEqualTo("dc-1");
    assertThat(result.maskedCardNumber()).isEqualTo("****1234");
    assertThat(result.totalAmount()).isEqualByComparingTo("100.00");
    assertThat(result.totalCommission()).isEqualByComparingTo("1.50");
    assertThat(result.debitedAccounts()).containsExactly(entry);
    assertThat(result.processedAt()).isEqualTo(PROCESSED_AT);
    assertThat(entry.accountId()).isEqualTo("acc-1");
    assertThat(entry.maskedAccountNumber()).isEqualTo("****5678");
    assertThat(entry.debitedAmount()).isEqualByComparingTo("100.00");
    assertThat(entry.appliedCommission()).isEqualByComparingTo("1.50");
    assertThat(entry.previousBalance()).isEqualByComparingTo("500.00");
    assertThat(entry.newBalance()).isEqualByComparingTo("400.00");
  }

  @Test
  @DisplayName("Should be equal when every field matches")
  void shouldBeEqualWhenEveryFieldMatches() {
    // Arrange
    DebitCardPaymentResult first = DebitCardPaymentResult.builder()
            .debitCardId("dc-1")
            .build();
    DebitCardPaymentResult second = DebitCardPaymentResult.builder()
            .debitCardId("dc-1")
            .build();

    // Act / Assert
    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }
}
