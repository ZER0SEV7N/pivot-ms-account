package com.pe.pivot.account.application.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FindAccountByIdUseCaseImpl.
 *
 * <p>Verifies the centralized lookup behavior and the not-found translation into a domain
 * exception.
 */
class FindAccountByIdUseCaseImplTest {

  private static final String ACCOUNT_ID = "account-1";

  private AccountRepositoryPort accountRepository;
  private FindAccountByIdUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepositoryPort.class);
    useCase = new FindAccountByIdUseCaseImpl(accountRepository);
  }

  @Test
  @DisplayName("Should emit the account when the repository finds it")
  void shouldEmitAccountWhenFound() {
    // Arrange
    BankAccount account = BankAccount.builder()
        .id(ACCOUNT_ID)
        .accountType(AccountType.SAVINGS)
        .status(AccountStatus.ACTIVE)
        .build();
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Maybe.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.findById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    observer.assertValueCount(1);
  }

  @Test
  @DisplayName("Should emit an AccountNotFoundException when the repository is empty")
  void shouldFailWhenAccountIsNotFound() {
    // Arrange
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<BankAccount> observer = useCase.findById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(AccountNotFoundException.class);
    observer.assertError(error -> error.getMessage().contains(ACCOUNT_ID));
  }

  @Test
  @DisplayName("Should propagate a repository failure")
  void shouldPropagateRepositoryFailure() {
    // Arrange
    when(accountRepository.findById(ACCOUNT_ID))
        .thenReturn(Maybe.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<BankAccount> observer = useCase.findById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }
}
