package com.pe.pivot.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DeleteAccountUseCaseImpl.
 *
 * <p>Verifies the soft-delete (logical closure) rules: an account can only be closed when it is
 * not already closed and its balance is zero.
 */
class DeleteAccountUseCaseImplTest {

  private static final String ACCOUNT_ID = "account-1";

  private AccountRepositoryPort accountRepository;
  private FindAccountByIdUseCase findAccountByIdUseCase;
  private DeleteAccountUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepositoryPort.class);
    findAccountByIdUseCase = mock(FindAccountByIdUseCase.class);
    useCase = new DeleteAccountUseCaseImpl(accountRepository, findAccountByIdUseCase);
  }

  @Test
  @DisplayName("Should logically close an account with a zero balance")
  void shouldCloseAccountWithZeroBalance() {
    // Arrange
    BankAccount account = accountWithBalance(BigDecimal.ZERO);
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));
    when(accountRepository.save(account)).thenReturn(Single.just(account));

    // Act
    TestObserver<Void> observer = useCase.delete(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    assertThat(account.getUpdatedAt()).isNotNull();
    verify(accountRepository).save(account);
  }

  @Test
  @DisplayName("Should treat a null balance as zero and close the account")
  void shouldCloseAccountWithNullBalance() {
    // Arrange
    BankAccount account = accountWithBalance(null);
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));
    when(accountRepository.save(account)).thenReturn(Single.just(account));

    // Act
    TestObserver<Void> observer = useCase.delete(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    verify(accountRepository).save(account);
  }

  @Test
  @DisplayName("Should reject closing an account that is already closed")
  void shouldRejectAlreadyClosedAccount() {
    // Arrange
    BankAccount account = accountWithBalance(BigDecimal.ZERO);
    account.setStatus(AccountStatus.CLOSED);
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));

    // Act
    TestObserver<Void> observer = useCase.delete(ACCOUNT_ID).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("already CLOSED"));
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should reject closing an account with a positive balance")
  void shouldRejectAccountWithPositiveBalance() {
    // Arrange
    BankAccount account = accountWithBalance(new BigDecimal("0.01"));
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));

    // Act
    TestObserver<Void> observer = useCase.delete(ACCOUNT_ID).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("remaining balance"));
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate a not-found failure from the lookup use case")
  void shouldPropagateNotFoundFailure() {
    // Arrange
    when(findAccountByIdUseCase.findById(ACCOUNT_ID))
        .thenReturn(Single.error(new AccountNotFoundException("not found")));

    // Act
    TestObserver<Void> observer = useCase.delete(ACCOUNT_ID).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(AccountNotFoundException.class);
  }

  @Test
  @DisplayName("Should propagate a repository failure while persisting the closure")
  void shouldPropagateSaveFailure() {
    // Arrange
    BankAccount account = accountWithBalance(BigDecimal.ZERO);
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(account));
    when(accountRepository.save(account))
        .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<Void> observer = useCase.delete(ACCOUNT_ID).test();

    // Assert
    observer.assertNotComplete();
    observer.assertError(IllegalStateException.class);
  }

  private BankAccount accountWithBalance(BigDecimal balance) {
    return BankAccount.builder()
        .id(ACCOUNT_ID)
        .status(AccountStatus.ACTIVE)
        .balance(balance)
        .build();
  }
}
