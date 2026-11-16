package com.pe.pivot.account.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.application.support.initializers.AccountBalancePolicy;
import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BalanceUseCaseImpl.
 *
 * <p>Validates that the use case delegates movement rules to the domain policy and persists the
 * updated account only when the operation succeeds.
 */
class BalanceUseCaseImplTest {

  private static final String ACCOUNT_ID = "account-1";

  private AccountRepositoryPort accountRepository;
  private FindAccountByIdUseCase findAccountByIdUseCase;
  private AccountBalancePolicy balancePolicy;
  private BalanceUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepositoryPort.class);
    findAccountByIdUseCase = mock(FindAccountByIdUseCase.class);
    balancePolicy = mock(AccountBalancePolicy.class);
    useCase = new BalanceUseCaseImpl(accountRepository, findAccountByIdUseCase, balancePolicy);
  }

  @Test
  @DisplayName("Should apply the movement and persist the updated account")
  void shouldApplyMovementAndPersistAccount() {
    // Arrange
    BankAccount account = account();
    BigDecimal amount = new BigDecimal("100.00");
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));
    when(accountRepository.save(account)).thenReturn(Single.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.updateBalance(ACCOUNT_ID,
        OperationType.DEPOSIT, amount).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    observer.assertValueCount(1);
    verify(balancePolicy).applyMovement(account, OperationType.DEPOSIT, amount);
    verify(accountRepository).save(account);
  }

  @Test
  @DisplayName("Should fail when the account does not exist")
  void shouldFailWhenAccountDoesNotExist() {
    // Arrange
    when(findAccountByIdUseCase.findById(ACCOUNT_ID))
        .thenReturn(Single.error(new AccountNotFoundException("not found")));

    // Act
    TestObserver<BankAccount> observer = useCase.updateBalance(ACCOUNT_ID,
        OperationType.DEPOSIT, BigDecimal.TEN).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(AccountNotFoundException.class);
    verify(balancePolicy, never()).applyMovement(any(), any(), any());
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate a business rule failure from the balance policy")
  void shouldPropagateBusinessRuleFailureFromPolicy() {
    // Arrange
    BankAccount account = account();
    doThrow(new BusinessRuleException("insufficient funds"))
        .when(balancePolicy).applyMovement(account, OperationType.WITHDRAWAL,
            new BigDecimal("9999.00"));
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.updateBalance(ACCOUNT_ID,
        OperationType.WITHDRAWAL, new BigDecimal("9999.00")).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate a repository failure while saving the updated account")
  void shouldPropagateSaveFailure() {
    // Arrange
    BankAccount account = account();
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));
    when(accountRepository.save(account))
        .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<BankAccount> observer = useCase.updateBalance(ACCOUNT_ID,
        OperationType.DEPOSIT, BigDecimal.TEN).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }

  private BankAccount account() {
    return BankAccount.builder()
        .id(ACCOUNT_ID)
        .status(AccountStatus.ACTIVE)
        .balance(new BigDecimal("500.00"))
        .build();
  }
}
