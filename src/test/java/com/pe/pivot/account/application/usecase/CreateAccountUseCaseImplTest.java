package com.pe.pivot.account.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.application.support.initializers.AccountDomainInitializer;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.exception.DuplicateAccountException;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.CreditClientPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.strategy.AccountValidationStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CreateAccountUseCaseImpl.
 *
 * <p>Validates the account creation orchestration, including customer verification, credit card
 * checks, strategy selection, domain initialization, and duplicate account protection.
 */
class CreateAccountUseCaseImplTest {

  private static final String CUSTOMER_ID = "customer-1";
  private static final String ACCOUNT_HASH = "account-hash";

  private AccountRepositoryPort accountRepository;
  private CustomerClientPort customerClient;
  private CreditClientPort creditClient;
  private AccountValidationStrategy validationStrategy;
  private AccountDomainInitializer accountInitializer;
  private CreateAccountUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepositoryPort.class);
    customerClient = mock(CustomerClientPort.class);
    creditClient = mock(CreditClientPort.class);
    validationStrategy = mock(AccountValidationStrategy.class);
    accountInitializer = mock(AccountDomainInitializer.class);
    useCase = new CreateAccountUseCaseImpl(accountRepository, customerClient, creditClient,
        List.of(validationStrategy), accountInitializer);
  }

  @Test
  @DisplayName("Should create a personal account when the customer is active and data is valid")
  void shouldCreatePersonalAccountWhenCustomerIsActive() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount saved = newAccount();
    saved.setId("account-1");

    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("PERSONAL")).thenReturn(true);
    BankAccount account = newAccount();
    when(validationStrategy.validate(customer, account, List.of(), false))
        .thenReturn(Completable.complete());
    when(accountRepository.existsByAccountNumberHash(ACCOUNT_HASH))
        .thenReturn(Single.just(false));
    when(accountRepository.save(account)).thenReturn(Single.just(saved));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(saved);
    observer.assertValueCount(1);
    verify(accountInitializer).initializeAndValidate(customer, account);
    verify(creditClient, never()).hasActiveCreditCard(any());
  }

  @Test
  @DisplayName("Should require an active credit card for a VIP customer")
  void shouldCheckCreditCardForVipCustomer() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", "VIP");
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(creditClient.hasActiveCreditCard(CUSTOMER_ID)).thenReturn(Single.just(true));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("PERSONAL")).thenReturn(true);
    when(validationStrategy.validate(customer, account, List.of(), true))
        .thenReturn(Completable.complete());
    when(accountRepository.existsByAccountNumberHash(ACCOUNT_HASH))
        .thenReturn(Single.just(false));
    when(accountRepository.save(account)).thenReturn(Single.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    verify(creditClient).hasActiveCreditCard(CUSTOMER_ID);
  }

  @Test
  @DisplayName("Should check the credit card for a PYME customer")
  void shouldCheckCreditCardForPymeCustomer() {
    // Arrange
    CustomerSummary customer = customer("BUSINESS", "PYME");
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(creditClient.hasActiveCreditCard(CUSTOMER_ID)).thenReturn(Single.just(false));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("BUSINESS")).thenReturn(true);
    when(validationStrategy.validate(customer, account, List.of(), false))
        .thenReturn(Completable.complete());
    when(accountRepository.existsByAccountNumberHash(ACCOUNT_HASH))
        .thenReturn(Single.just(false));
    when(accountRepository.save(account)).thenReturn(Single.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verify(creditClient).hasActiveCreditCard(CUSTOMER_ID);
  }

  @Test
  @DisplayName("Should fail when the customer does not exist in ms-customer")
  void shouldFailWhenCustomerDoesNotExist() {
    // Arrange
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(CustomerNotFoundException.class);
    observer.assertError(error -> error.getMessage().contains(CUSTOMER_ID));
  }

  @Test
  @DisplayName("Should fail when the customer is inactive")
  void shouldFailWhenCustomerIsInactive() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    customer.setStatus("INACTIVE");
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("inactive"));
  }

  @Test
  @DisplayName("Should fail when no validation strategy supports the customer type")
  void shouldFailWhenNoStrategySupportsCustomerType() {
    // Arrange
    CustomerSummary customer = customer("UNKNOWN", null);
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("UNKNOWN")).thenReturn(false);

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("Unsupported customer type"));
  }

  @Test
  @DisplayName("Should fail when the selected strategy rejects the creation")
  void shouldFailWhenStrategyRejectsCreation() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("PERSONAL")).thenReturn(true);
    when(validationStrategy.validate(customer, account, List.of(), false))
        .thenReturn(Completable.error(new BusinessRuleException("rule violated")));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should fail when the account number already exists")
  void shouldFailWhenAccountNumberAlreadyExists() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("PERSONAL")).thenReturn(true);
    when(validationStrategy.validate(customer, account, List.of(), false))
        .thenReturn(Completable.complete());
    when(accountRepository.existsByAccountNumberHash(ACCOUNT_HASH))
        .thenReturn(Single.just(true));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(DuplicateAccountException.class);
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate an initialization failure raised by the domain initializer")
  void shouldPropagateInitializerFailure() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("PERSONAL")).thenReturn(true);
    when(validationStrategy.validate(customer, account, List.of(), false))
        .thenReturn(Completable.complete());
    doThrow(new BusinessRuleException("invalid opening balance"))
        .when(accountInitializer).initializeAndValidate(customer, account);

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate a repository failure while saving the account")
  void shouldPropagateSaveFailure() {
    // Arrange
    CustomerSummary customer = customer("PERSONAL", null);
    BankAccount account = newAccount();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(customer));
    when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.empty());
    when(validationStrategy.supports("PERSONAL")).thenReturn(true);
    when(validationStrategy.validate(customer, account, List.of(), false))
        .thenReturn(Completable.complete());
    when(accountRepository.existsByAccountNumberHash(ACCOUNT_HASH))
        .thenReturn(Single.just(false));
    when(accountRepository.save(account))
        .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<BankAccount> observer = useCase.create(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }

  private CustomerSummary customer(String customerType, String profile) {
    return CustomerSummary.builder()
        .id(CUSTOMER_ID)
        .customerType(customerType)
        .profile(profile)
        .documentNumber("12345678")
        .status("ACTIVE")
        .build();
  }

  private BankAccount newAccount() {
    return BankAccount.builder()
        .accountNumber("191-00000000-0-01")
        .accountNumberHash(ACCOUNT_HASH)
        .customerId(CUSTOMER_ID)
        .accountType(AccountType.SAVINGS)
        .build();
  }
}
