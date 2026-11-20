package com.pe.pivot.account.application.support.debitcard;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DebitCardAccountValidator}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to validate accounts linked to a debit card")
class DebitCardAccountValidatorTest {

  private static final String CUSTOMER_ID = "cust-1";

  @Mock
  private FindAccountByIdUseCase findAccountByIdUseCase;

  private DebitCardAccountValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DebitCardAccountValidator(findAccountByIdUseCase);
  }

  @Test
  @DisplayName("Should be validated when every linked account is owned and active")
  void shouldBeValidatedWhenEveryLinkedAccountIsOwnedAndActive() {
    // (happy path)
    // Arrange
    when(findAccountByIdUseCase.findById("acc-1"))
            .thenReturn(Single.just(account("acc-1", CUSTOMER_ID, AccountStatus.ACTIVE)));
    when(findAccountByIdUseCase.findById("acc-2"))
            .thenReturn(Single.just(account("acc-2", CUSTOMER_ID, AccountStatus.ACTIVE)));

    // Act
    TestObserver<Void> observer = validator
            .validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1", "acc-2"))
            .test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
  }

  @Test
  @DisplayName("Should be completed when no account is linked")
  void shouldBeCompletedWhenNoAccountIsLinked() {
    // Arrange - no linked accounts

    // Act
    TestObserver<Void> observer = validator
            .validateLinkedAccounts(CUSTOMER_ID, List.of())
            .test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verifyNoInteractions(findAccountByIdUseCase);
  }

  @Test
  @DisplayName("Should be rejected when an account belongs to another customer")
  void shouldBeRejectedWhenAccountBelongsToAnotherCustomer() {
    // (error: account ownership mismatch)
    // Arrange
    when(findAccountByIdUseCase.findById("acc-1"))
            .thenReturn(Single.just(account("acc-1", "other-cust", AccountStatus.ACTIVE)));

    // Act
    TestObserver<Void> observer = validator
            .validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1"))
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("does not belong to customer"));
  }

  @Test
  @DisplayName("Should be rejected when an account is not ACTIVE")
  void shouldBeRejectedWhenAccountIsNotActive() {
    // (error: inactive account)
    // Arrange
    when(findAccountByIdUseCase.findById("acc-1"))
            .thenReturn(Single.just(account("acc-1", CUSTOMER_ID, AccountStatus.BLOCKED)));

    // Act
    TestObserver<Void> observer = validator
            .validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1"))
            .test();

    // Assert
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("non-ACTIVE account"));
  }

  @Test
  @DisplayName("Should be propagated when the account lookup fails")
  void shouldBePropagatedWhenAccountLookupFails() {
    // (error: account not found)
    // Arrange
    when(findAccountByIdUseCase.findById("acc-1"))
            .thenReturn(Single.error(new IllegalArgumentException("account not found")));

    // Act
    TestObserver<Void> observer = validator
            .validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1"))
            .test();

    // Assert
    observer.assertError(IllegalArgumentException.class);
    observer.assertNotComplete();
  }

  private BankAccount account(String id, String customerId, AccountStatus status) {
    return BankAccount.builder()
            .id(id)
            .customerId(customerId)
            .status(status)
            .build();
  }
}
