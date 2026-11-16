package com.pe.pivot.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OperationType balance arithmetic strategies.
 */
class OperationTypeTest {

  @Test
  @DisplayName("Should add the deposit and subtract the commission")
  void shouldCalculateDepositBalance() {
    // Act
    BigDecimal result = OperationType.DEPOSIT.calculateNewBalance(
        new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("5.00"));

    // Assert
    assertThat(result).isEqualByComparingTo("145.00");
  }

  @Test
  @DisplayName("Should reject a deposit that cannot cover the commission")
  void shouldRejectDepositThatCannotCoverCommission() {
    // Act / Assert
    assertThatThrownBy(() -> OperationType.DEPOSIT.calculateNewBalance(
        BigDecimal.ZERO, new BigDecimal("1.00"), new BigDecimal("5.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("insufficient to cover the transaction commission");
  }

  @Test
  @DisplayName("Should subtract the withdrawal and commission from the balance")
  void shouldCalculateWithdrawalBalance() {
    // Act
    BigDecimal result = OperationType.WITHDRAWAL.calculateNewBalance(
        new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("5.00"));

    // Assert
    assertThat(result).isEqualByComparingTo("55.00");
  }

  @Test
  @DisplayName("Should reject a withdrawal when the balance is insufficient")
  void shouldRejectWithdrawalWithInsufficientBalance() {
    // Act / Assert
    assertThatThrownBy(() -> OperationType.WITHDRAWAL.calculateNewBalance(
        new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("5.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Insufficient funds");
  }
}
