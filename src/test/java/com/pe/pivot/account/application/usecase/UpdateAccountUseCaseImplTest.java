package com.pe.pivot.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for UpdateAccountUseCaseImpl.
 *
 * <p>Validates partial update semantics: non-null incoming fields replace existing values while
 * null fields preserve the current state.
 */
class UpdateAccountUseCaseImplTest {

  private static final String ACCOUNT_ID = "account-1";

  private AccountRepositoryPort accountRepository;
  private FindAccountByIdUseCase findAccountByIdUseCase;
  private UpdateAccountUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepositoryPort.class);
    findAccountByIdUseCase = mock(FindAccountByIdUseCase.class);
    useCase = new UpdateAccountUseCaseImpl(accountRepository, findAccountByIdUseCase);
  }

  @Test
  @DisplayName("Should update every supplied field and persist the account")
  void shouldUpdateAllSuppliedFields() {
    // Arrange
    BankAccount existing = existingAccount();
    BankAccount incoming = BankAccount.builder()
        .currency("USD")
        .balance(new BigDecimal("900.00"))
        .maintenanceFee(new BigDecimal("12.00"))
        .maxMonthlyMovements(20)
        .allowedMovementDay(15)
        .holders(List.of("holder-new"))
        .authorizedSigners(List.of("signer-new"))
        .status(AccountStatus.BLOCKED)
        .build();
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(existing));
    when(accountRepository.save(existing)).thenReturn(Single.just(existing));

    // Act
    TestObserver<BankAccount> observer = useCase.updateAccount(ACCOUNT_ID, incoming).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(existing);
    assertThat(existing.getCurrency()).isEqualTo("USD");
    assertThat(existing.getBalance()).isEqualByComparingTo("900.00");
    assertThat(existing.getMaintenanceFee()).isEqualByComparingTo("12.00");
    assertThat(existing.getMaxMonthlyMovements()).isEqualTo(20);
    assertThat(existing.getAllowedMovementDay()).isEqualTo(15);
    assertThat(existing.getHolders()).containsExactly("holder-new");
    assertThat(existing.getAuthorizedSigners()).containsExactly("signer-new");
    assertThat(existing.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    assertThat(existing.getUpdatedAt()).isNotNull();
    verify(accountRepository).save(existing);
  }

  @Test
  @DisplayName("Should preserve existing values when the incoming fields are null")
  void shouldPreserveValuesWhenIncomingFieldsAreNull() {
    // Arrange
    BankAccount existing = existingAccount();
    BankAccount incoming = BankAccount.builder().build();
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(existing));
    when(accountRepository.save(existing)).thenReturn(Single.just(existing));

    // Act
    TestObserver<BankAccount> observer = useCase.updateAccount(ACCOUNT_ID, incoming).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(existing.getCurrency()).isEqualTo("PEN");
    assertThat(existing.getBalance()).isEqualByComparingTo("100.00");
    assertThat(existing.getMaintenanceFee()).isEqualByComparingTo("5.00");
    assertThat(existing.getMaxMonthlyMovements()).isEqualTo(10);
    assertThat(existing.getAllowedMovementDay()).isEqualTo(28);
    assertThat(existing.getHolders()).containsExactly("holder-old");
    assertThat(existing.getAuthorizedSigners()).containsExactly("signer-old");
    assertThat(existing.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(existing.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should fail when the account to update does not exist")
  void shouldFailWhenAccountDoesNotExist() {
    // Arrange
    when(findAccountByIdUseCase.findById(ACCOUNT_ID))
        .thenReturn(Single.error(new AccountNotFoundException("not found")));

    // Act
    TestObserver<BankAccount> observer = useCase
        .updateAccount(ACCOUNT_ID, BankAccount.builder().build()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(AccountNotFoundException.class);
    verify(accountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate a repository failure while saving the updated account")
  void shouldPropagateSaveFailure() {
    // Arrange
    BankAccount existing = existingAccount();
    when(findAccountByIdUseCase.findById(ACCOUNT_ID)).thenReturn(Single.just(existing));
    when(accountRepository.save(existing))
        .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<BankAccount> observer = useCase
        .updateAccount(ACCOUNT_ID, BankAccount.builder().build()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }

  private BankAccount existingAccount() {
    return BankAccount.builder()
        .id(ACCOUNT_ID)
        .accountNumber("191-00000000-0-01")
        .accountType(AccountType.SAVINGS)
        .currency("PEN")
        .balance(new BigDecimal("100.00"))
        .maintenanceFee(new BigDecimal("5.00"))
        .maxMonthlyMovements(10)
        .allowedMovementDay(28)
        .holders(List.of("holder-old"))
        .authorizedSigners(List.of("signer-old"))
        .status(AccountStatus.ACTIVE)
        .build();
  }
}
