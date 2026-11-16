package com.pe.pivot.account.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PersonalAccountStrategy.
 *
 * <p>Validates single-account ownership rules and VIP credit card requirements for personal
 * customers.
 */
class PersonalAccountStrategyTest {

  private PersonalAccountStrategy strategy;
  private CustomerSummary customer;

  @BeforeEach
  void setUp() {
    strategy = new PersonalAccountStrategy();
    customer = CustomerSummary.builder()
        .id("customer-1")
        .customerType("PERSONAL")
        .status("ACTIVE")
        .build();
  }

  @Test
  @DisplayName("Should support only the PERSONAL customer type")
  void shouldSupportOnlyPersonalCustomerType() {
    // Act / Assert
    assertThat(strategy.supports("PERSONAL")).isTrue();
    assertThat(strategy.supports("BUSINESS")).isFalse();
    assertThat(strategy.supports(null)).isFalse();
  }

  @Test
  @DisplayName("Should allow a SAVINGS account when the customer has no active SAVINGS account")
  void shouldAllowFirstSavingsAccount() {
    // Arrange
    BankAccount newAccount = account(AccountType.SAVINGS);
    List<BankAccount> existing = List.of(account(AccountType.CHECKING));

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, existing, false).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should reject a SAVINGS account when an active SAVINGS account already exists")
  void shouldRejectDuplicateActiveSavingsAccount() {
    // Arrange
    BankAccount newAccount = account(AccountType.SAVINGS);
    BankAccount existingAccount = account(AccountType.SAVINGS);
    existingAccount.setStatus(AccountStatus.ACTIVE);

    // Act
    TestObserver<Void> observer = strategy
        .validate(customer, newAccount, List.of(existingAccount), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("maximum of one active SAVINGS"));
  }

  @Test
  @DisplayName("Should allow a new SAVINGS account when the previous one is CLOSED")
  void shouldAllowSavingsWhenPreviousIsClosed() {
    // Arrange
    BankAccount newAccount = account(AccountType.SAVINGS);
    BankAccount closedAccount = account(AccountType.SAVINGS);
    closedAccount.setStatus(AccountStatus.CLOSED);

    // Act
    TestObserver<Void> observer = strategy
        .validate(customer, newAccount, List.of(closedAccount), false).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should reject a CHECKING account when an active CHECKING account already exists")
  void shouldRejectDuplicateActiveCheckingAccount() {
    // Arrange
    BankAccount newAccount = account(AccountType.CHECKING);
    BankAccount existingAccount = account(AccountType.CHECKING);
    existingAccount.setStatus(AccountStatus.BLOCKED);

    // Act
    TestObserver<Void> observer = strategy
        .validate(customer, newAccount, List.of(existingAccount), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("maximum of one active CHECKING"));
  }

  @Test
  @DisplayName("Should allow multiple FIXED_TERM accounts for the same customer")
  void shouldAllowMultipleFixedTermAccounts() {
    // Arrange
    BankAccount newAccount = account(AccountType.FIXED_TERM);
    BankAccount existingAccount = account(AccountType.FIXED_TERM);
    existingAccount.setStatus(AccountStatus.ACTIVE);

    // Act
    TestObserver<Void> observer = strategy
        .validate(customer, newAccount, List.of(existingAccount), false).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should reject a VIP SAVINGS account when the customer has no active credit card")
  void shouldRejectVipSavingsWithoutCreditCard() {
    // Arrange
    customer.setProfile("VIP");
    BankAccount newAccount = account(AccountType.SAVINGS);

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("must hold an active Credit Card"));
  }

  @Test
  @DisplayName("Should allow a VIP SAVINGS account when the customer has an active credit card")
  void shouldAllowVipSavingsWithCreditCard() {
    // Arrange
    customer.setProfile("VIP");
    BankAccount newAccount = account(AccountType.SAVINGS);

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), true).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should not require a credit card for a VIP CHECKING account")
  void shouldNotRequireCreditCardForVipChecking() {
    // Arrange
    customer.setProfile("VIP");
    BankAccount newAccount = account(AccountType.CHECKING);

    // Act
    TestObserver<Void> observer = strategy.validate(customer, newAccount, List.of(), false).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  private BankAccount account(AccountType accountType) {
    return BankAccount.builder().accountType(accountType).build();
  }
}
