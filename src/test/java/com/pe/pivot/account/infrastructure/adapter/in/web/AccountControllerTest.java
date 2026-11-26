package com.pe.pivot.account.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.port.in.CreateAccountUseCase;
import com.pe.pivot.account.domain.port.in.DeleteAccountUseCase;
import com.pe.pivot.account.domain.port.in.RetrieveAccountUseCase;
import com.pe.pivot.account.domain.port.in.UpdateAccountUseCase;
import com.pe.pivot.account.domain.port.in.UpdateBalanceUseCase;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.AccountController;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceUpdateRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.mapper.AccountWebMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for AccountController.
 *
 * <p>Validates each REST endpoint delegates to its use case and maps the result to the expected
 * HTTP status and payload.
 */
class AccountControllerTest {

  private static final String ACCOUNT_ID = "account-1";

  private CreateAccountUseCase createAccountUseCase;
  private RetrieveAccountUseCase retrieveAccountUseCase;
  private UpdateAccountUseCase updateAccountUseCase;
  private UpdateBalanceUseCase updateBalanceUseCase;
  private DeleteAccountUseCase deleteAccountUseCase;
  private AccountController controller;

  @BeforeEach
  void setUp() {
    createAccountUseCase = mock(CreateAccountUseCase.class);
    retrieveAccountUseCase = mock(RetrieveAccountUseCase.class);
    updateAccountUseCase = mock(UpdateAccountUseCase.class);
    updateBalanceUseCase = mock(UpdateBalanceUseCase.class);
    deleteAccountUseCase = mock(DeleteAccountUseCase.class);
    controller = new AccountController(createAccountUseCase, retrieveAccountUseCase,
        updateAccountUseCase, updateBalanceUseCase, deleteAccountUseCase,
        new AccountWebMapper());
  }

  @Test
  @DisplayName("Should create an account and respond with 201 and the unmasked number")
  void shouldCreateAccount() {
    // Arrange
    when(createAccountUseCase.create(any(BankAccount.class))).thenReturn(Single.just(domain()));

    // Act
    TestObserver<ResponseEntity<AccountResponse>> observer =
        controller.createAccount(request()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> response.getStatusCode().value() == HttpStatus.CREATED.value()
        && "191-00000000-0-01".equals(response.getBody().accountNumber()));
  }

  @Test
  @DisplayName("Should return all accounts")
  void shouldGetAllAccounts() {
    // Arrange
    when(retrieveAccountUseCase.findAll()).thenReturn(Flowable.just(domain()));

    // Act
    TestSubscriber<AccountResponse> observer = controller.getAllAccounts().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(response -> response.accountNumber().equals("*************0-01"));
  }

  @Test
  @DisplayName("Should return an account by id with status 200")
  void shouldGetAccountById() {
    // Arrange
    when(retrieveAccountUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(domain()));

    // Act
    TestObserver<ResponseEntity<AccountResponse>> observer =
        controller.getAccountById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> response.getStatusCode().value() == HttpStatus.OK.value()
        && ACCOUNT_ID.equals(response.getBody().id()));
  }

  @Test
  @DisplayName("Should return an account by account number with status 200")
  void shouldGetAccountByNumber() {
    // Arrange
    when(retrieveAccountUseCase.findByAccountNumber("191-00000000-0-01"))
        .thenReturn(Single.just(domain()));

    // Act
    TestObserver<ResponseEntity<AccountResponse>> observer =
        controller.getAccountByNumber("191-00000000-0-01").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> response.getStatusCode().value() == HttpStatus.OK.value());
  }

  @Test
  @DisplayName("Should return all accounts of a customer")
  void shouldGetAccountsByCustomerId() {
    // Arrange
    when(retrieveAccountUseCase.findByCustomerId("customer-1"))
        .thenReturn(Flowable.just(domain()));

    // Act
    TestSubscriber<AccountResponse> observer =
        controller.getAccountsByCustomerId("customer-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(response -> "customer-1".equals(response.customerId()));
  }

  @Test
  @DisplayName("Should update an account and respond with status 200")
  void shouldUpdateAccount() {
    // Arrange
    when(updateAccountUseCase.updateAccount(eq(ACCOUNT_ID), any(BankAccount.class)))
        .thenReturn(Single.just(domain()));

    // Act
    TestObserver<ResponseEntity<AccountResponse>> observer =
        controller.updateAccount(ACCOUNT_ID, request()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> response.getStatusCode().value() == HttpStatus.OK.value());
  }

  @Test
  @DisplayName("Should update an account balance and respond with status 200")
  void shouldUpdateAccountBalance() {
    // Arrange
    BalanceUpdateRequest request = BalanceUpdateRequest.builder()
        .operationType(OperationType.DEPOSIT)
        .amount(new BigDecimal("50.00"))
        .build();
    when(updateBalanceUseCase.updateBalance(ACCOUNT_ID, OperationType.DEPOSIT,
        new BigDecimal("50.00"))).thenReturn(Single.just(domain()));

    // Act
    TestObserver<ResponseEntity<AccountResponse>> observer =
        controller.updateAccountBalance(ACCOUNT_ID, request).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> response.getStatusCode().value() == HttpStatus.OK.value());
  }

  @Test
  @DisplayName("Should return the available balance of an account")
  void shouldGetAccountBalance() {
    // Arrange
    when(retrieveAccountUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(domain()));

    // Act
    TestObserver<BalanceResponse> observer = controller.getAccountBalance(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> ACCOUNT_ID.equals(response.accountId())
        && response.availableBalance().compareTo(new BigDecimal("100.00")) == 0
        && "*************0-01".equals(response.accountNumber()));
  }

  @Test
  @DisplayName("Should delete an account and complete without values")
  void shouldDeleteAccount() {
    // Arrange
    when(deleteAccountUseCase.delete(ACCOUNT_ID)).thenReturn(Completable.complete());

    // Act
    TestObserver<Void> observer = controller.deleteAccount(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertNoValues();
  }

  @Test
  @DisplayName("Should propagate a use case failure through the reactive response")
  void shouldPropagateUseCaseFailure() {
    // Arrange
    when(createAccountUseCase.create(any(BankAccount.class)))
        .thenReturn(Single.error(new BusinessRuleException("rule violated")));

    // Act
    TestObserver<ResponseEntity<AccountResponse>> observer =
        controller.createAccount(request()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
  }

  private AccountRequest request() {
    return AccountRequest.builder()
        .accountNumber("191-00000000-0-01")
        .customerId("customer-1")
        .accountType(AccountType.SAVINGS)
        .currency("PEN")
        .balance(new BigDecimal("100.00"))
        .build();
  }

  private BankAccount domain() {
    return BankAccount.builder()
        .id(ACCOUNT_ID)
        .accountNumber("191-00000000-0-01")
        .customerId("customer-1")
        .accountType(AccountType.SAVINGS)
        .currency("PEN")
        .balance(new BigDecimal("100.00"))
        .maintenanceFee(BigDecimal.ZERO)
        .minimumDailyAverage(BigDecimal.ZERO)
        .minOpeningAmount(BigDecimal.ZERO)
        .maxMonthlyMovements(5)
        .maxFreeMonthlyTransactions(3)
        .transactionCommissionFee(new BigDecimal("5.00"))
        .lastAppliedCommission(BigDecimal.ZERO)
        .currentMonthlyMovements(2)
        .allowedMovementDay(28)
        .holders(List.of("holder-1"))
        .authorizedSigners(List.of("signer-1"))
        .status(AccountStatus.ACTIVE)
        .build();
  }
}
