package com.pe.pivot.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BankAccount domain behavior.
 */
class BankAccountTest {

  @Test
  @DisplayName("Should consider an account closable when the balance is null")
  void shouldBeClosableWhenBalanceIsNull() {
    // Arrange
    BankAccount account = BankAccount.builder().balance(null).build();

    // Act / Assert
    assertThat(account.canBeClosed()).isTrue();
  }

  @Test
  @DisplayName("Should consider an account closable when the balance is zero")
  void shouldBeClosableWhenBalanceIsZero() {
    // Arrange
    BankAccount account = BankAccount.builder().balance(BigDecimal.ZERO).build();

    // Act / Assert
    assertThat(account.canBeClosed()).isTrue();
  }

  @Test
  @DisplayName("Should not consider an account closable when the balance is positive")
  void shouldNotBeClosableWhenBalanceIsPositive() {
    // Arrange
    BankAccount account = BankAccount.builder().balance(new BigDecimal("0.01")).build();

    // Act / Assert
    assertThat(account.canBeClosed()).isFalse();
  }

  @Test
  @DisplayName("Should report free transactions available when the limit is null")
  void shouldReportFreeTransactionsWhenLimitIsNull() {
    // Arrange
    BankAccount account = BankAccount.builder()
        .maxFreeMonthlyTransactions(null)
        .currentMonthlyMovements(99)
        .build();

    // Act / Assert
    assertThat(account.isFreeTransactionAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should report free transactions available when movements are below the limit")
  void shouldReportFreeTransactionsWhenBelowLimit() {
    // Arrange
    BankAccount account = BankAccount.builder()
        .maxFreeMonthlyTransactions(3)
        .currentMonthlyMovements(2)
        .build();

    // Act / Assert
    assertThat(account.isFreeTransactionAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should report free transactions available when movements are null")
  void shouldReportFreeTransactionsWhenMovementsAreNull() {
    // Arrange
    BankAccount account = BankAccount.builder()
        .maxFreeMonthlyTransactions(3)
        .currentMonthlyMovements(null)
        .build();

    // Act / Assert
    assertThat(account.isFreeTransactionAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should report no free transactions when the limit is reached")
  void shouldReportNoFreeTransactionsWhenLimitReached() {
    // Arrange
    BankAccount account = BankAccount.builder()
        .maxFreeMonthlyTransactions(3)
        .currentMonthlyMovements(3)
        .build();

    // Act / Assert
    assertThat(account.isFreeTransactionAvailable()).isFalse();
  }
}
