package com.pe.pivot.account.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BusinessAccountStrategy.
 *
 * <p>Validates account type restrictions, holder requirements, and PYME credit card rules for
 * business customers.
 */
class BusinessAccountStrategyTest {

  private BusinessAccountStrategy strategy;
  private CustomerSummary customer;

  @BeforeEach
  void setUp() {
    strategy = new BusinessAccountStrategy();
    customer = CustomerSummary.builder()
        .id("customer-1")
        .customerType("BUSINESS")
        .status("ACTIVE")
        .build();
  }

  /**
   * Verifies that the strategy only claims support for the BUSINESS customer type.
   */
  @Test
  @DisplayName("Should support the BUSINESS customer type ignoring case")
  void shouldSupportBusinessCustomerType() {
    // Act / Assert
    assertThat(strategy.supports("BUSINESS")).isTrue();
    assertThat(strategy.supports("business")).isTrue();
    assertThat(strategy.supports("PERSONAL")).isFalse();
    assertThat(strategy.supports(null)).isFalse();
  }

  @Test
  @DisplayName("Should reject a SAVINGS account for a business customer")
  void shouldRejectSavingsAccount() {
    // Arrange
    BankAccount newAccount = account(AccountType.SAVINGS, List.of("holder-1"));

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("not allowed to open SAVINGS"));
  }

  @Test
  @DisplayName("Should reject a FIXED_TERM account for a business customer")
  void shouldRejectFixedTermAccount() {
    // Arrange
    BankAccount newAccount = account(AccountType.FIXED_TERM, List.of("holder-1"));

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Should reject a CHECKING account without holders")
  void shouldRejectCheckingAccountWithoutHolders() {
    // Arrange
    BankAccount newAccount = account(AccountType.CHECKING, null);

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("at least one account holder"));
  }

  @Test
  @DisplayName("Should reject a CHECKING account with an empty holders list")
  void shouldRejectCheckingAccountWithEmptyHolders() {
    // Arrange
    BankAccount newAccount = account(AccountType.CHECKING, Collections.emptyList());

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Should allow a valid CHECKING account for a non-PYME business customer")
  void shouldAllowCheckingForNonPymeCustomer() {
    // Arrange
    BankAccount newAccount = account(AccountType.CHECKING, List.of("holder-1"));

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should reject a PYME CHECKING account without an active credit card")
  void shouldRejectPymeCheckingWithoutCreditCard() {
    // Arrange
    customer.setProfile("PYME");
    BankAccount newAccount = account(AccountType.CHECKING, List.of("holder-1"));

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("must hold an active Credit Card"));
  }

  @Test
  @DisplayName("Should zero the maintenance fee for a PYME CHECKING account with a credit card")
  void shouldZeroMaintenanceFeeForPymeCheckingWithCreditCard() {
    // Arrange
    customer.setProfile("PYME");
    BankAccount newAccount = account(AccountType.CHECKING, List.of("holder-1"));
    newAccount.setMaintenanceFee(new BigDecimal("15.00"));

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), true).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(newAccount.getMaintenanceFee()).isEqualByComparingTo("0");
  }

  private BankAccount account(AccountType accountType, List<String> holders) {
    return BankAccount.builder()
        .accountType(accountType)
        .holders(holders)
        .build();
  }
}
