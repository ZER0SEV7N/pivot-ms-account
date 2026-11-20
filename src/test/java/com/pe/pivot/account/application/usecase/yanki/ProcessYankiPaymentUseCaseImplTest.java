package com.pe.pivot.account.application.usecase.yanki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.model.yanki.YankiPaymentCommand;
import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import com.pe.pivot.account.domain.port.in.UpdateBalanceUseCase;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ProcessYankiPaymentUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to process a Yanki payment")
class ProcessYankiPaymentUseCaseImplTest {

  private static final String SENDER_CARD = "dc-sender";
  private static final String RECEIVER_CARD = "dc-receiver";
  private static final String SENDER_ACCOUNT = "acc-sender";
  private static final String RECEIVER_ACCOUNT = "acc-receiver";
  private static final BigDecimal AMOUNT = new BigDecimal("30.00");

  @Mock
  private DebitCardRepositoryPort debitCardRepository;

  @Mock
  private UpdateBalanceUseCase updateBalanceUseCase;

  private ProcessYankiPaymentUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new ProcessYankiPaymentUseCaseImpl(debitCardRepository, updateBalanceUseCase);
  }

  @Test
  @DisplayName("Should be completed when both parties are linked and funded")
  void shouldBeCompletedWhenBothPartiesAreLinkedAndFunded() {
    // (happy path)
    // Arrange
    when(debitCardRepository.findById(SENDER_CARD))
            .thenReturn(Maybe.just(activeCard(SENDER_CARD, SENDER_ACCOUNT)));
    when(debitCardRepository.findById(RECEIVER_CARD))
            .thenReturn(Maybe.just(activeCard(RECEIVER_CARD, RECEIVER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(SENDER_ACCOUNT, OperationType.WITHDRAWAL, AMOUNT))
            .thenReturn(Single.just(account(SENDER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(RECEIVER_ACCOUNT, OperationType.DEPOSIT, AMOUNT))
            .thenReturn(Single.just(account(RECEIVER_ACCOUNT)));

    // Act
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("COMPLETED");
    assertThat(outcome.debitedAccountId()).isEqualTo(SENDER_ACCOUNT);
    assertThat(outcome.creditedAccountId()).isEqualTo(RECEIVER_ACCOUNT);
    assertThat(outcome.failureReason()).isNull();
    assertThat(outcome.transactionId()).isEqualTo("tx-1");
    assertThat(outcome.amount()).isEqualByComparingTo("30.00");
    assertThat(outcome.processedAt()).isNotNull();
    verify(updateBalanceUseCase).updateBalance(SENDER_ACCOUNT, OperationType.WITHDRAWAL, AMOUNT);
    verify(updateBalanceUseCase).updateBalance(RECEIVER_ACCOUNT, OperationType.DEPOSIT, AMOUNT);
  }

  @Test
  @DisplayName("Should be credited only when the sender debit card is missing")
  void shouldBeCreditedOnlyWhenSenderDebitCardIsMissing() {
    // Arrange
    when(debitCardRepository.findById(RECEIVER_CARD))
            .thenReturn(Maybe.just(activeCard(RECEIVER_CARD, RECEIVER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(RECEIVER_ACCOUNT, OperationType.DEPOSIT, AMOUNT))
            .thenReturn(Single.just(account(RECEIVER_ACCOUNT)));

    // Act
    YankiPaymentCommand command = command();
    command = YankiPaymentCommand.builder()
            .transactionId(command.transactionId())
            .receiverPhoneNumber(command.receiverPhoneNumber())
            .senderDebitCardId(null)
            .receiverDebitCardId(RECEIVER_CARD)
            .currency(command.currency())
            .amount(command.amount())
            .build();
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("COMPLETED");
    assertThat(outcome.debitedAccountId()).isNull();
    assertThat(outcome.creditedAccountId()).isEqualTo(RECEIVER_ACCOUNT);
    verify(updateBalanceUseCase, never())
            .updateBalance(SENDER_ACCOUNT, OperationType.WITHDRAWAL, AMOUNT);
  }

  @Test
  @DisplayName("Should be debited only when the receiver debit card is missing")
  void shouldBeDebitedOnlyWhenReceiverDebitCardIsMissing() {
    // Arrange
    when(debitCardRepository.findById(SENDER_CARD))
            .thenReturn(Maybe.just(activeCard(SENDER_CARD, SENDER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(SENDER_ACCOUNT, OperationType.WITHDRAWAL, AMOUNT))
            .thenReturn(Single.just(account(SENDER_ACCOUNT)));

    // Act
    YankiPaymentCommand command = YankiPaymentCommand.builder()
            .transactionId("tx-1")
            .senderPhoneNumber("987654321")
            .senderDebitCardId(SENDER_CARD)
            .receiverDebitCardId("   ")
            .currency("PEN")
            .amount(AMOUNT)
            .build();
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("COMPLETED");
    assertThat(outcome.debitedAccountId()).isEqualTo(SENDER_ACCOUNT);
    assertThat(outcome.creditedAccountId()).isNull();
    verify(updateBalanceUseCase, never())
            .updateBalance(RECEIVER_ACCOUNT, OperationType.DEPOSIT, AMOUNT);
  }

  @Test
  @DisplayName("Should be failed when the sender debit card does not exist")
  void shouldBeFailedWhenSenderDebitCardDoesNotExist() {
    // (error: sender card not found)
    // Arrange
    when(debitCardRepository.findById(SENDER_CARD)).thenReturn(Maybe.empty());

    // Act
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("FAILED");
    assertThat(outcome.debitedAccountId()).isNull();
    assertThat(outcome.creditedAccountId()).isNull();
    assertThat(outcome.failureReason()).contains("not found");
    verifyNoInteractions(updateBalanceUseCase);
  }

  @Test
  @DisplayName("Should be failed when the sender debit card is not active")
  void shouldBeFailedWhenSenderDebitCardIsNotActive() {
    // (error: blocked sender card)
    // Arrange
    DebitCard blocked = activeCard(SENDER_CARD, SENDER_ACCOUNT);
    blocked.setStatus(DebitCardStatus.BLOCKED);
    when(debitCardRepository.findById(SENDER_CARD)).thenReturn(Maybe.just(blocked));

    // Act
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("FAILED");
    assertThat(outcome.failureReason()).contains("not ACTIVE");
    verifyNoInteractions(updateBalanceUseCase);
  }

  @Test
  @DisplayName("Should be compensated when the receiver credit fails after the sender debit")
  void shouldBeCompensatedWhenReceiverCreditFailsAfterSenderDebit() {
    // (error: receiver deposit rejected)
    // Arrange
    when(debitCardRepository.findById(SENDER_CARD))
            .thenReturn(Maybe.just(activeCard(SENDER_CARD, SENDER_ACCOUNT)));
    when(debitCardRepository.findById(RECEIVER_CARD))
            .thenReturn(Maybe.just(activeCard(RECEIVER_CARD, RECEIVER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(SENDER_ACCOUNT, OperationType.WITHDRAWAL, AMOUNT))
            .thenReturn(Single.just(account(SENDER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(RECEIVER_ACCOUNT, OperationType.DEPOSIT, AMOUNT))
            .thenReturn(Single.error(new IllegalStateException("deposit rejected")));
    when(updateBalanceUseCase.updateBalance(SENDER_ACCOUNT, OperationType.DEPOSIT, AMOUNT))
            .thenReturn(Single.just(account(SENDER_ACCOUNT)));

    // Act
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("FAILED");
    assertThat(outcome.failureReason()).isEqualTo("deposit rejected");
    verify(updateBalanceUseCase).updateBalance(SENDER_ACCOUNT, OperationType.DEPOSIT, AMOUNT);
  }

  @Test
  @DisplayName("Should be failed without compensation when only the receiver is processed")
  void shouldBeFailedWithoutCompensationWhenOnlyReceiverIsProcessed() {
    // (error: receiver deposit rejected with no prior debit)
    // Arrange
    YankiPaymentCommand command = YankiPaymentCommand.builder()
            .transactionId("tx-1")
            .receiverPhoneNumber("999888777")
            .senderDebitCardId("")
            .receiverDebitCardId(RECEIVER_CARD)
            .currency("PEN")
            .amount(AMOUNT)
            .build();
    when(debitCardRepository.findById(RECEIVER_CARD))
            .thenReturn(Maybe.just(activeCard(RECEIVER_CARD, RECEIVER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(RECEIVER_ACCOUNT, OperationType.DEPOSIT, AMOUNT))
            .thenReturn(Single.error(new IllegalStateException("deposit rejected")));

    // Act
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(observer.values().get(0).status()).isEqualTo("FAILED");
    verify(updateBalanceUseCase, never())
            .updateBalance(SENDER_ACCOUNT, OperationType.DEPOSIT, AMOUNT);
  }

  @Test
  @DisplayName("Should be failed when the sender withdrawal is rejected")
  void shouldBeFailedWhenSenderWithdrawalIsRejected() {
    // (error: insufficient sender funds)
    // Arrange
    when(debitCardRepository.findById(SENDER_CARD))
            .thenReturn(Maybe.just(activeCard(SENDER_CARD, SENDER_ACCOUNT)));
    when(updateBalanceUseCase.updateBalance(SENDER_ACCOUNT, OperationType.WITHDRAWAL, AMOUNT))
            .thenReturn(Single.error(new IllegalStateException("insufficient funds")));

    // Act
    TestObserver<YankiPaymentOutcome> observer = useCase.process(command()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    YankiPaymentOutcome outcome = observer.values().get(0);
    assertThat(outcome.status()).isEqualTo("FAILED");
    assertThat(outcome.failureReason()).isEqualTo("insufficient funds");
    verify(debitCardRepository, never()).findById(RECEIVER_CARD);
  }

  private YankiPaymentCommand command() {
    return YankiPaymentCommand.builder()
            .transactionId("tx-1")
            .senderPhoneNumber("987654321")
            .receiverPhoneNumber("999888777")
            .senderDebitCardId(SENDER_CARD)
            .receiverDebitCardId(RECEIVER_CARD)
            .currency("PEN")
            .amount(AMOUNT)
            .description("Yanki payment")
            .requestedAt(Instant.parse("2026-01-15T10:00:00Z"))
            .build();
  }

  private DebitCard activeCard(String id, String primaryAccountId) {
    return DebitCard.builder()
            .id(id)
            .primaryAccountId(primaryAccountId)
            .status(DebitCardStatus.ACTIVE)
            .build();
  }

  private BankAccount account(String id) {
    return BankAccount.builder()
            .id(id)
            .status(AccountStatus.ACTIVE)
            .balance(new BigDecimal("100.00"))
            .build();
  }
}
