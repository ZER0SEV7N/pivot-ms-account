package com.pe.pivot.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RetrieveAccountUseCaseImpl.
 *
 * <p>Covers all read operations: all accounts, lookup by id, lookup by account number, and lookup
 * by customer id.
 */
class RetrieveAccountUseCaseImplTest {

  private static final String ACCOUNT_NUMBER = "12345678";
  private static final String CUSTOMER_ID = "customer-1";

  private AccountRepositoryPort accountRepository;
  private FindAccountByIdUseCase findAccountByIdUseCase;
  private CryptoUtil cryptoUtil;
  private RetrieveAccountUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepositoryPort.class);
    findAccountByIdUseCase = mock(FindAccountByIdUseCase.class);
    cryptoUtil = new CryptoUtil("unit-test-secret-key");
    useCase = new RetrieveAccountUseCaseImpl(accountRepository, findAccountByIdUseCase, cryptoUtil);
  }

  @Test
  @DisplayName("Should emit all accounts returned by the repository")
  void shouldFindAllAccounts() {
    // Arrange
    BankAccount first = account("account-1");
    BankAccount second = account("account-2");
    when(accountRepository.findAll()).thenReturn(Flowable.just(first, second));

    // Act
    TestSubscriber<BankAccount> observer = useCase.findAll().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValues(first, second);
    observer.assertValueCount(2);
  }

  @Test
  @DisplayName("Should delegate the lookup by id to FindAccountByIdUseCase")
  void shouldDelegateFindById() {
    // Arrange
    BankAccount account = account("account-1");
    when(findAccountByIdUseCase.findById("account-1")).thenReturn(Single.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.findById("account-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    verify(findAccountByIdUseCase).findById("account-1");
  }

  @Test
  @DisplayName("Should find an account by its plain account number using the SHA-256 hash")
  void shouldFindByAccountNumber() {
    // Arrange
    BankAccount account = account("account-1");
    String expectedHash = cryptoUtil.hashSha256(ACCOUNT_NUMBER);
    when(accountRepository.findByAccountNumberHash(expectedHash))
        .thenReturn(Maybe.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.findByAccountNumber(ACCOUNT_NUMBER).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    verify(accountRepository).findByAccountNumberHash(expectedHash);
    assertThat(expectedHash).hasSize(64);
  }

  @Test
  @DisplayName("Should emit a masked AccountNotFoundException when the account number is unknown")
  void shouldFailWhenAccountNumberIsUnknown() {
    // Arrange
    String expectedHash = cryptoUtil.hashSha256(ACCOUNT_NUMBER);
    when(accountRepository.findByAccountNumberHash(expectedHash)).thenReturn(Maybe.empty());

    // Act
    TestObserver<BankAccount> observer = useCase.findByAccountNumber(ACCOUNT_NUMBER).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(AccountNotFoundException.class);
    observer.assertError(error -> error.getMessage().contains("****5678"));
    assertThat(cryptoUtil.hashSha256(ACCOUNT_NUMBER)).isEqualTo(expectedHash);
  }

  @Test
  @DisplayName("Should propagate a repository failure during the account number lookup")
  void shouldPropagateAccountNumberLookupFailure() {
    // Arrange
    String expectedHash = cryptoUtil.hashSha256(ACCOUNT_NUMBER);
    when(accountRepository.findByAccountNumberHash(expectedHash))
        .thenReturn(Maybe.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<BankAccount> observer = useCase.findByAccountNumber(ACCOUNT_NUMBER).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should emit every account belonging to the requested customer")
  void shouldFindAccountsByCustomerId() {
    // Arrange
    BankAccount first = account("account-1");
    BankAccount second = account("account-2");
    when(accountRepository.findByCustomerId(CUSTOMER_ID))
        .thenReturn(Flowable.just(first, second));

    // Act
    TestSubscriber<BankAccount> observer = useCase.findByCustomerId(CUSTOMER_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValues(first, second);
    verify(accountRepository).findByCustomerId(CUSTOMER_ID);
  }

  private BankAccount account(String id) {
    return BankAccount.builder()
        .id(id)
        .accountNumber(ACCOUNT_NUMBER)
        .customerId(CUSTOMER_ID)
        .accountType(AccountType.SAVINGS)
        .status(AccountStatus.ACTIVE)
        .build();
  }
}
