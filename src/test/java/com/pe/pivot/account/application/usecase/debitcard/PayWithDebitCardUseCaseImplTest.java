package com.pe.pivot.account.application.usecase.debitcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.application.support.debitcard.CascadingDebitPolicy;
import com.pe.pivot.account.application.support.debitcard.CascadingExecution;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.model.debit.DebitedAccountEntry;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PayWithDebitCardUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to pay with a debit card")
class PayWithDebitCardUseCaseImplTest {

  private static final String CARD_ID = "dc-1";
  private static final String PRIMARY_ACCOUNT = "acc-1";
  private static final String SECONDARY_ACCOUNT = "acc-2";
  private static final BigDecimal AMOUNT = new BigDecimal("100.00");

  @Mock
  private DebitCardRepositoryPort debitCardRepository;

  @Mock
  private FindAccountByIdUseCase findAccountByIdUseCase;

  @Mock
  private AccountRepositoryPort accountRepository;

  @Mock
  private CascadingDebitPolicy cascadingDebitPolicy;

  private PayWithDebitCardUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new PayWithDebitCardUseCaseImpl(debitCardRepository, findAccountByIdUseCase,
            accountRepository, cascadingDebitPolicy);
  }

  @Test
  @DisplayName("Should be applied and persisted when the card is active and funded")
  void shouldBeAppliedAndPersistedWhenCardIsActiveAndFunded() {
    // (happy path)
    // Arrange
    DebitCard card = card();
    BankAccount primary = account(PRIMARY_ACCOUNT, "300.00");
    BankAccount secondary = account(SECONDARY_ACCOUNT, "300.00");
    DebitCardPaymentResult result = result();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT)).thenReturn(Single.just(primary));
    when(findAccountByIdUseCase.findById(SECONDARY_ACCOUNT)).thenReturn(Single.just(secondary));
    when(cascadingDebitPolicy.execute(card, List.of(primary, secondary), AMOUNT))
            .thenReturn(new CascadingExecution(List.of(primary), result));
    when(accountRepository.save(primary)).thenReturn(Single.just(primary));

    // Act
    TestObserver<DebitCardPaymentResult> observer = useCase.pay(CARD_ID, AMOUNT).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(result);
    assertThat(observer.values().get(0).totalAmount()).isEqualByComparingTo("100.00");
    verify(accountRepository).save(primary);
    verify(accountRepository, never()).save(secondary);
  }

  @Test
  @DisplayName("Should be rejected when the amount is null")
  void shouldBeRejectedWhenAmountIsNull() {
    // (error: missing amount)
    // Act
    TestObserver<DebitCardPaymentResult> observer = useCase.pay(CARD_ID, null).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("greater than zero"));
    verifyNoInteractions(debitCardRepository);
  }

  @Test
  @DisplayName("Should be rejected when the amount is zero")
  void shouldBeRejectedWhenAmountIsZero() {
    // (error: zero amount)
    // Act
    TestObserver<DebitCardPaymentResult> observer =
            useCase.pay(CARD_ID, BigDecimal.ZERO).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verifyNoInteractions(debitCardRepository);
  }

  @Test
  @DisplayName("Should be rejected when the amount is negative")
  void shouldBeRejectedWhenAmountIsNegative() {
    // (error: negative amount)
    // Act
    TestObserver<DebitCardPaymentResult> observer =
            useCase.pay(CARD_ID, new BigDecimal("-1.00")).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verifyNoInteractions(debitCardRepository);
  }

  @Test
  @DisplayName("Should be rejected when the debit card does not exist")
  void shouldBeRejectedWhenDebitCardDoesNotExist() {
    // (error: debit card not found)
    // Arrange
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<DebitCardPaymentResult> observer = useCase.pay(CARD_ID, AMOUNT).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(DebitCardNotFoundException.class);
    verifyNoInteractions(findAccountByIdUseCase);
  }

  @Test
  @DisplayName("Should be rejected when the debit card is not active")
  void shouldBeRejectedWhenDebitCardIsNotActive() {
    // (error: blocked debit card)
    // Arrange
    DebitCard blocked = card();
    blocked.setStatus(DebitCardStatus.BLOCKED);
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(blocked));

    // Act
    TestObserver<DebitCardPaymentResult> observer = useCase.pay(CARD_ID, AMOUNT).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("not ACTIVE"));
    verifyNoInteractions(findAccountByIdUseCase);
  }

  @Test
  @DisplayName("Should be rejected when the cascading policy finds insufficient funds")
  void shouldBeRejectedWhenCascadingPolicyFindsInsufficientFunds() {
    // (error: insufficient combined funds)
    // Arrange
    DebitCard card = card();
    BankAccount primary = account(PRIMARY_ACCOUNT, "10.00");
    BankAccount secondary = account(SECONDARY_ACCOUNT, "10.00");
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT)).thenReturn(Single.just(primary));
    when(findAccountByIdUseCase.findById(SECONDARY_ACCOUNT)).thenReturn(Single.just(secondary));
    when(cascadingDebitPolicy.execute(eq(card), any(), eq(AMOUNT)))
            .thenThrow(new BusinessRuleException("Insufficient combined funds."));

    // Act
    TestObserver<DebitCardPaymentResult> observer = useCase.pay(CARD_ID, AMOUNT).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("Insufficient combined funds"));
    verifyNoInteractions(accountRepository);
  }

  @Test
  @DisplayName("Should be propagated when an account update fails")
  void shouldBePropagatedWhenAccountUpdateFails() {
    // (error: persistence failure)
    // Arrange
    DebitCard card = card();
    BankAccount primary = account(PRIMARY_ACCOUNT, "300.00");
    BankAccount secondary = account(SECONDARY_ACCOUNT, "300.00");
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT)).thenReturn(Single.just(primary));
    when(findAccountByIdUseCase.findById(SECONDARY_ACCOUNT)).thenReturn(Single.just(secondary));
    when(cascadingDebitPolicy.execute(card, List.of(primary, secondary), AMOUNT))
            .thenReturn(new CascadingExecution(List.of(primary), result()));
    when(accountRepository.save(primary))
            .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<DebitCardPaymentResult> observer = useCase.pay(CARD_ID, AMOUNT).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
  }

  private DebitCard card() {
    return DebitCard.builder()
            .id(CARD_ID)
            .cardNumber("4557891234567890")
            .primaryAccountId(PRIMARY_ACCOUNT)
            .secondaryAccountIds(List.of(SECONDARY_ACCOUNT))
            .status(DebitCardStatus.ACTIVE)
            .build();
  }

  private BankAccount account(String id, String balance) {
    return BankAccount.builder()
            .id(id)
            .accountNumber("191-" + id + "-0-01")
            .accountType(AccountType.SAVINGS)
            .status(AccountStatus.ACTIVE)
            .balance(new BigDecimal(balance))
            .build();
  }

  private DebitCardPaymentResult result() {
    return DebitCardPaymentResult.builder()
            .debitCardId(CARD_ID)
            .maskedCardNumber("************7890")
            .totalAmount(AMOUNT)
            .totalCommission(BigDecimal.ZERO)
            .debitedAccounts(List.of(DebitedAccountEntry.builder()
                    .accountId(PRIMARY_ACCOUNT)
                    .maskedAccountNumber("************0-01")
                    .debitedAmount(AMOUNT)
                    .appliedCommission(BigDecimal.ZERO)
                    .previousBalance(new BigDecimal("300.00"))
                    .newBalance(new BigDecimal("200.00"))
                    .build()))
            .processedAt(Instant.parse("2026-01-15T10:00:00Z"))
            .build();
  }
}
