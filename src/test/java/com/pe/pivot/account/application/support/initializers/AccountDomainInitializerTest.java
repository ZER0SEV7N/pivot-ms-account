package com.pe.pivot.account.application.support.initializers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.infrastructure.config.AccountRulesProperties;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountDomainInitializer.
 *
 * <p>Verifies default metadata initialization, product-specific financial rules, and minimum
 * opening balance validation.
 */
class AccountDomainInitializerTest {

  private AccountRulesProperties rules;
  private CryptoUtil cryptoUtil;
  private AccountDomainInitializer initializer;

  @BeforeEach
  void setUp() {
    rules = new AccountRulesProperties();
    cryptoUtil = new CryptoUtil("unit-test-secret-key");
    initializer = new AccountDomainInitializer(rules, cryptoUtil);
  }

  @Test
  @DisplayName("Should initialize default metadata and SAVINGS rules for a non-VIP customer")
  void shouldInitializeSavingsDefaultsForNonVipCustomer() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = BankAccount.builder().accountType(AccountType.SAVINGS).build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getAccountNumber()).startsWith("191-").endsWith("-0-01");
    assertThat(account.getAccountNumberHash())
        .isEqualTo(cryptoUtil.hashSha256(account.getAccountNumber()));
    assertThat(account.getCurrency()).isEqualTo("PEN");
    assertThat(account.getBalance()).isEqualByComparingTo("0");
    assertThat(account.getCurrentMonthlyMovements()).isZero();
    assertThat(account.getLastAppliedCommission()).isEqualByComparingTo("0");
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(account.getMaxFreeMonthlyTransactions()).isEqualTo(3);
    assertThat(account.getTransactionCommissionFee()).isEqualByComparingTo("5.00");
    assertThat(account.getHolders()).containsExactly("12345678");
    assertThat(account.getAuthorizedSigners()).isEmpty();
    assertThat(account.getMaintenanceFee()).isEqualByComparingTo("0");
    assertThat(account.getMaxMonthlyMovements()).isEqualTo(5);
    assertThat(account.getMinimumDailyAverage()).isEqualByComparingTo("0");
    assertThat(account.getMinOpeningAmount()).isEqualByComparingTo("0");
    assertThat(account.getCreatedAt()).isNotNull();
    assertThat(account.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should apply VIP minimums to a SAVINGS account")
  void shouldApplyVipRulesToSavingsAccount() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", "VIP");
    BankAccount account = BankAccount.builder()
        .accountType(AccountType.SAVINGS)
        .balance(new BigDecimal("600.00"))
        .build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getMinimumDailyAverage()).isEqualByComparingTo("500.00");
    assertThat(account.getMinOpeningAmount()).isEqualByComparingTo("500.00");
    assertThat(account.getMaintenanceFee()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should reject a SAVINGS opening balance below the VIP minimum")
  void shouldRejectBalanceBelowVipMinimum() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", "VIP");
    BankAccount account = BankAccount.builder()
        .accountType(AccountType.SAVINGS)
        .balance(new BigDecimal("100.00"))
        .build();

    // Act / Assert
    assertThatThrownBy(() -> initializer.initializeAndValidate(customer, account))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be less than the required minimum opening amount");
  }

  @Test
  @DisplayName("Should preserve explicitly supplied values instead of applying defaults")
  void shouldPreserveExplicitlySuppliedValues() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = BankAccount.builder()
        .accountNumber("CUSTOM-NUMBER")
        .accountType(AccountType.SAVINGS)
        .currency("USD")
        .balance(new BigDecimal("1000.00"))
        .status(AccountStatus.BLOCKED)
        .maxFreeMonthlyTransactions(10)
        .transactionCommissionFee(new BigDecimal("2.50"))
        .maintenanceFee(new BigDecimal("1.00"))
        .maxMonthlyMovements(9)
        .minimumDailyAverage(new BigDecimal("50.00"))
        .minOpeningAmount(new BigDecimal("100.00"))
        .holders(List.of("holder-1"))
        .authorizedSigners(List.of("signer-1"))
        .build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getAccountNumber()).isEqualTo("CUSTOM-NUMBER");
    assertThat(account.getAccountNumberHash())
        .isEqualTo(cryptoUtil.hashSha256("CUSTOM-NUMBER"));
    assertThat(account.getCurrency()).isEqualTo("USD");
    assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
    assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    assertThat(account.getMaxFreeMonthlyTransactions()).isEqualTo(10);
    assertThat(account.getTransactionCommissionFee()).isEqualByComparingTo("2.50");
    // SAVINGS accounts always enforce a zero maintenance fee regardless of the input.
    assertThat(account.getMaintenanceFee()).isEqualByComparingTo("0");
    assertThat(account.getMaxMonthlyMovements()).isEqualTo(9);
    // Non-VIP SAVINGS accounts always enforce a zero minimum daily average.
    assertThat(account.getMinimumDailyAverage()).isEqualByComparingTo("0");
    assertThat(account.getMinOpeningAmount()).isEqualByComparingTo("100.00");
    assertThat(account.getHolders()).containsExactly("holder-1");
    assertThat(account.getAuthorizedSigners()).containsExactly("signer-1");
  }

  @Test
  @DisplayName("Should apply the default maintenance fee to a non-PYME CHECKING account")
  void shouldApplyCheckingRulesForNonPymeCustomer() {
    // Arrange
    CustomerSummary customer = customer("BUSINESS", null);
    BankAccount account = BankAccount.builder()
        .accountType(AccountType.CHECKING)
        .holders(List.of("holder-1"))
        .build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getMaintenanceFee()).isEqualByComparingTo("15.00");
    assertThat(account.getMaxMonthlyMovements()).isNull();
    assertThat(account.getMinimumDailyAverage()).isEqualByComparingTo("0");
    assertThat(account.getMinOpeningAmount()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should not assign a maintenance fee to a PYME CHECKING account")
  void shouldNotAssignMaintenanceFeeToPymeCheckingAccount() {
    // Arrange
    CustomerSummary customer = customer("BUSINESS", "PYME");
    BankAccount account = BankAccount.builder()
        .accountType(AccountType.CHECKING)
        .holders(List.of("holder-1"))
        .build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getMaintenanceFee()).isNull();
    assertThat(account.getMaxMonthlyMovements()).isNull();
  }

  @Test
  @DisplayName("Should apply FIXED_TERM defaults including the movement day")
  void shouldApplyFixedTermDefaults() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = BankAccount.builder()
        .accountType(AccountType.FIXED_TERM)
        .build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getMaintenanceFee()).isEqualByComparingTo("0");
    assertThat(account.getMaxMonthlyMovements()).isEqualTo(1);
    assertThat(account.getAllowedMovementDay()).isEqualTo(28);
    assertThat(account.getMinimumDailyAverage()).isEqualByComparingTo("0");
    assertThat(account.getMinOpeningAmount()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should preserve an explicitly supplied FIXED_TERM movement day")
  void shouldPreserveFixedTermMovementDay() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = BankAccount.builder()
        .accountType(AccountType.FIXED_TERM)
        .allowedMovementDay(15)
        .build();

    // Act
    initializer.initializeAndValidate(customer, account);

    // Assert
    assertThat(account.getAllowedMovementDay()).isEqualTo(15);
  }

  private CustomerSummary customer(String customerType, String profile) {
    return CustomerSummary.builder()
        .id("customer-1")
        .customerType(customerType)
        .profile(profile)
        .documentNumber("12345678")
        .status("ACTIVE")
        .build();
  }
}
