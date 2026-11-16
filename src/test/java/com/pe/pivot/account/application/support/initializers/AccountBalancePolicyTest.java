package com.pe.pivot.account.application.support.initializers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountBalancePolicy.
 *
 * <p>Validates movement preconditions, commission resolution, and balance mutation for deposits
 * and withdrawals.
 */
class AccountBalancePolicyTest {

  private AccountBalancePolicy policy;

  @BeforeEach
  void setUp() {
    policy = new AccountBalancePolicy();
  }

  @Test
  @DisplayName("Should deposit funds without commission when free transactions are available")
  void shouldDepositWithoutCommissionWhenFreeTransactionsAvailable() {
    // Arrange
    BankAccount account = activeAccount();
    account.setBalance(new BigDecimal("100.00"));
    account.setMaxFreeMonthlyTransactions(3);
    account.setCurrentMonthlyMovements(0);
    account.setTransactionCommissionFee(new BigDecimal("5.00"));

    // Act
    policy.applyMovement(account, OperationType.DEPOSIT, new BigDecimal("50.00"));

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo("150.00");
    assertThat(account.getCurrentMonthlyMovements()).isEqualTo(1);
    assertThat(account.getLastAppliedCommission()).isEqualByComparingTo("0");
    assertThat(account.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should withdraw funds without commission when free transactions are available")
  void shouldWithdrawWithoutCommissionWhenFreeTransactionsAvailable() {
    // Arrange
    BankAccount account = activeAccount();
    account.setBalance(new BigDecimal("100.00"));
    account.setMaxFreeMonthlyTransactions(3);
    account.setCurrentMonthlyMovements(1);

    // Act
    policy.applyMovement(account, OperationType.WITHDRAWAL, new BigDecimal("40.00"));

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo("60.00");
    assertThat(account.getCurrentMonthlyMovements()).isEqualTo(2);
    assertThat(account.getLastAppliedCommission()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should apply the commission when the free transaction quota is exhausted")
  void shouldApplyCommissionWhenFreeQuotaExhausted() {
    // Arrange
    BankAccount account = activeAccount();
    account.setBalance(new BigDecimal("100.00"));
    account.setMaxFreeMonthlyTransactions(0);
    account.setCurrentMonthlyMovements(0);
    account.setTransactionCommissionFee(new BigDecimal("5.00"));

    // Act
    policy.applyMovement(account, OperationType.DEPOSIT, new BigDecimal("50.00"));

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo("145.00");
    assertThat(account.getLastAppliedCommission()).isEqualByComparingTo("5.00");
  }

  @Test
  @DisplayName("Should treat a null commission fee as zero when no free transactions remain")
  void shouldTreatNullCommissionAsZero() {
    // Arrange
    BankAccount account = activeAccount();
    account.setBalance(new BigDecimal("100.00"));
    account.setMaxFreeMonthlyTransactions(0);
    account.setCurrentMonthlyMovements(0);
    account.setTransactionCommissionFee(null);

    // Act
    policy.applyMovement(account, OperationType.DEPOSIT, new BigDecimal("10.00"));

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo("110.00");
    assertThat(account.getLastAppliedCommission()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should treat a null current balance as zero before applying a deposit")
  void shouldTreatNullBalanceAsZero() {
    // Arrange
    BankAccount account = activeAccount();
    account.setBalance(null);
    account.setCurrentMonthlyMovements(null);
    account.setMaxMonthlyMovements(null);

    // Act
    policy.applyMovement(account, OperationType.DEPOSIT, new BigDecimal("25.00"));

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo("25.00");
    assertThat(account.getCurrentMonthlyMovements()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should reject movements on an account that is not ACTIVE")
  void shouldRejectMovementWhenAccountIsNotActive() {
    // Arrange
    BankAccount account = activeAccount();
    account.setStatus(AccountStatus.BLOCKED);

    // Act / Assert
    assertThatThrownBy(() -> policy.applyMovement(account, OperationType.DEPOSIT,
        BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not ACTIVE");
  }

  @Test
  @DisplayName("Should reject a null transaction amount")
  void shouldRejectNullAmount() {
    // Arrange
    BankAccount account = activeAccount();

    // Act / Assert
    assertThatThrownBy(() -> policy.applyMovement(account, OperationType.DEPOSIT, null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("greater than zero");
  }

  @Test
  @DisplayName("Should reject a zero transaction amount")
  void shouldRejectZeroAmount() {
    // Arrange
    BankAccount account = activeAccount();

    // Act / Assert
    assertThatThrownBy(() -> policy.applyMovement(account, OperationType.DEPOSIT,
        BigDecimal.ZERO))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("greater than zero");
  }

  @Test
  @DisplayName("Should reject a movement when the monthly movement limit is reached")
  void shouldRejectMovementWhenMonthlyLimitReached() {
    // Arrange
    BankAccount account = activeAccount();
    account.setMaxMonthlyMovements(2);
    account.setCurrentMonthlyMovements(2);

    // Act / Assert
    assertThatThrownBy(() -> policy.applyMovement(account, OperationType.DEPOSIT,
        BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Maximum monthly movements");
  }

  @Test
  @DisplayName("Should allow a FIXED_TERM movement on the configured day of the month")
  void shouldAllowFixedTermMovementOnConfiguredDay() {
    // Arrange
    int today = LocalDate.now(ZoneOffset.UTC).getDayOfMonth();
    BankAccount account = activeAccount();
    account.setAccountType(AccountType.FIXED_TERM);
    account.setAllowedMovementDay(today);
    account.setMaxMonthlyMovements(1);
    account.setCurrentMonthlyMovements(0);
    account.setBalance(new BigDecimal("100.00"));

    // Act
    policy.applyMovement(account, OperationType.DEPOSIT, new BigDecimal("10.00"));

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo("110.00");
  }

  @Test
  @DisplayName("Should reject a FIXED_TERM movement outside the configured day")
  void shouldRejectFixedTermMovementOutsideConfiguredDay() {
    // Arrange
    int today = LocalDate.now(ZoneOffset.UTC).getDayOfMonth();
    int differentDay = today == 1 ? 2 : 1;
    BankAccount account = activeAccount();
    account.setAccountType(AccountType.FIXED_TERM);
    account.setAllowedMovementDay(differentDay);
    account.setMaxMonthlyMovements(1);
    account.setCurrentMonthlyMovements(0);

    // Act / Assert
    assertThatThrownBy(() -> policy.applyMovement(account, OperationType.DEPOSIT,
        BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("FIXED_TERM accounts only allow movements");
  }

  @Test
  @DisplayName("Should reject a withdrawal that exceeds the available balance plus commission")
  void shouldRejectWithdrawalWithInsufficientFunds() {
    // Arrange
    BankAccount account = activeAccount();
    account.setBalance(new BigDecimal("50.00"));
    account.setMaxFreeMonthlyTransactions(3);

    // Act / Assert
    assertThatThrownBy(() -> policy.applyMovement(account, OperationType.WITHDRAWAL,
        new BigDecimal("60.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Insufficient funds");
  }

  private BankAccount activeAccount() {
    return BankAccount.builder()
        .id("account-1")
        .status(AccountStatus.ACTIVE)
        .balance(BigDecimal.ZERO)
        .build();
  }
}
