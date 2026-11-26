package com.pe.pivot.account.infrastructure.adapter.in.web.debit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.model.debit.DebitedAccountEntry;
import com.pe.pivot.account.domain.port.in.debit.CreateDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.DeleteDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.PayWithDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.RetrieveDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.UpdateDebitCardUseCase;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.mapper.AccountWebMapper;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardPaymentRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardPaymentResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardUpdateRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.mapper.DebitCardWebMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebitCardController}.
 */
@DisplayName("Should be able to expose the debit card REST API")
class DebitCardControllerTest {

  private static final String CARD_ID = "dc-1";
  private static final String CARD_NUMBER = "4557891234567890";
  private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");

  private CreateDebitCardUseCase createDebitCardUseCase;
  private RetrieveDebitCardUseCase retrieveDebitCardUseCase;
  private UpdateDebitCardUseCase updateDebitCardUseCase;
  private DeleteDebitCardUseCase deleteDebitCardUseCase;
  private PayWithDebitCardUseCase payWithDebitCardUseCase;
  private DebitCardController controller;

  @BeforeEach
  void setUp() {
    createDebitCardUseCase = mock(CreateDebitCardUseCase.class);
    retrieveDebitCardUseCase = mock(RetrieveDebitCardUseCase.class);
    updateDebitCardUseCase = mock(UpdateDebitCardUseCase.class);
    deleteDebitCardUseCase = mock(DeleteDebitCardUseCase.class);
    payWithDebitCardUseCase = mock(PayWithDebitCardUseCase.class);
    controller = new DebitCardController(createDebitCardUseCase, retrieveDebitCardUseCase,
            updateDebitCardUseCase, deleteDebitCardUseCase, payWithDebitCardUseCase,
            new DebitCardWebMapper(), new AccountWebMapper());
  }

  @Test
  @DisplayName("Should be created and returned with the unmasked card number")
  void shouldBeCreatedAndReturnedWithUnmaskedCardNumber() {
    // (happy path)
    // Arrange
    when(createDebitCardUseCase.create(any(DebitCard.class)))
            .thenReturn(Single.just(card()));

    // Act
    TestObserver<DebitCardResponse> observer = controller.createDebitCard(request()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> CARD_ID.equals(response.id())
            && CARD_NUMBER.equals(response.cardNumber())
            && DebitCardStatus.ACTIVE == response.status());
  }

  @Test
  @DisplayName("Should be listed with masked card numbers")
  void shouldBeListedWithMaskedCardNumbers() {
    // Arrange
    when(retrieveDebitCardUseCase.findAll()).thenReturn(Flowable.just(card()));

    // Act
    TestSubscriber<DebitCardResponse> observer = controller.getAllDebitCards().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(response -> "************7890".equals(response.cardNumber()));
  }

  @Test
  @DisplayName("Should be returned by id when it exists")
  void shouldBeReturnedByIdWhenItExists() {
    // Arrange
    when(retrieveDebitCardUseCase.findById(CARD_ID)).thenReturn(Single.just(card()));

    // Act
    TestObserver<DebitCardResponse> observer = controller.getDebitCardById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> CARD_ID.equals(response.id()));
  }

  @Test
  @DisplayName("Should be returned by card number when it exists")
  void shouldBeReturnedByCardNumberWhenItExists() {
    // Arrange
    when(retrieveDebitCardUseCase.findByCardNumber(CARD_NUMBER)).thenReturn(Single.just(card()));

    // Act
    TestObserver<DebitCardResponse> observer =
            controller.getDebitCardByNumber(CARD_NUMBER).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> CARD_ID.equals(response.id()));
  }

  @Test
  @DisplayName("Should be listed by customer id")
  void shouldBeListedByCustomerId() {
    // Arrange
    when(retrieveDebitCardUseCase.findByCustomerId("cust-1")).thenReturn(Flowable.just(card()));

    // Act
    TestSubscriber<DebitCardResponse> observer =
            controller.getDebitCardsByCustomerId("cust-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(response -> "cust-1".equals(response.customerId()));
  }

  @Test
  @DisplayName("Should return the primary account balance of the debit card")
  void shouldReturnPrimaryAccountBalanceOfDebitCard() {
    // Arrange
    when(retrieveDebitCardUseCase.getPrimaryAccountBalance(CARD_ID))
            .thenReturn(Single.just(account()));

    // Act
    TestObserver<BalanceResponse> observer =
            controller.getDebitCardPrimaryBalance(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> "acc-1".equals(response.accountId())
            && response.availableBalance().compareTo(new BigDecimal("250.00")) == 0
            && response.queriedAt() != null);
  }

  @Test
  @DisplayName("Should be updated and returned masked")
  void shouldBeUpdatedAndReturnedMasked() {
    // Arrange
    DebitCard updated = card();
    updated.setStatus(DebitCardStatus.BLOCKED);
    when(updateDebitCardUseCase.update(eq(CARD_ID), any(DebitCard.class)))
            .thenReturn(Single.just(updated));

    // Act
    TestObserver<DebitCardResponse> observer = controller
            .updateDebitCard(CARD_ID, updateRequest()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> DebitCardStatus.BLOCKED == response.status()
            && "************7890".equals(response.cardNumber()));
  }

  @Test
  @DisplayName("Should be paid and returned with the full breakdown")
  void shouldBePaidAndReturnedWithFullBreakdown() {
    // Arrange
    when(payWithDebitCardUseCase.pay(CARD_ID, new BigDecimal("100.00")))
            .thenReturn(Single.just(paymentResult()));

    // Act
    TestObserver<DebitCardPaymentResponse> observer =
            controller.payWithDebitCard(CARD_ID, paymentRequest()).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> CARD_ID.equals(response.debitCardId())
            && response.totalAmount().compareTo(new BigDecimal("100.00")) == 0
            && response.debitedAccounts().size() == 1
            && "acc-1".equals(response.debitedAccounts().get(0).accountId()));
  }

  @Test
  @DisplayName("Should be deleted and complete without values")
  void shouldBeDeletedAndCompleteWithoutValues() {
    // Arrange
    when(deleteDebitCardUseCase.delete(CARD_ID)).thenReturn(Completable.complete());

    // Act
    TestObserver<Void> observer = controller.deleteDebitCard(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertNoValues();
  }

  @Test
  @DisplayName("Should propagate a use case failure through the reactive response")
  void shouldPropagateUseCaseFailureThroughReactiveResponse() {
    // (error: business rule violated)
    // Arrange
    when(createDebitCardUseCase.create(any(DebitCard.class)))
            .thenReturn(Single.error(new BusinessRuleException("rule violated")));

    // Act
    TestObserver<DebitCardResponse> observer = controller.createDebitCard(request()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
  }

  private DebitCardRequest request() {
    return DebitCardRequest.builder()
            .customerId("cust-1")
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .build();
  }

  private DebitCardUpdateRequest updateRequest() {
    return DebitCardUpdateRequest.builder()
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .status(DebitCardStatus.BLOCKED)
            .build();
  }

  private DebitCardPaymentRequest paymentRequest() {
    return DebitCardPaymentRequest.builder()
            .amount(new BigDecimal("100.00"))
            .build();
  }

  private DebitCard card() {
    return DebitCard.builder()
            .id(CARD_ID)
            .cardNumber(CARD_NUMBER)
            .customerId("cust-1")
            .customerName("Ada Lovelace")
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .status(DebitCardStatus.ACTIVE)
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();
  }

  private BankAccount account() {
    return BankAccount.builder()
            .id("acc-1")
            .accountNumber("191-00000000-0-01")
            .accountType(AccountType.SAVINGS)
            .status(AccountStatus.ACTIVE)
            .currency("PEN")
            .balance(new BigDecimal("250.00"))
            .maxMonthlyMovements(5)
            .currentMonthlyMovements(2)
            .build();
  }

  private DebitCardPaymentResult paymentResult() {
    return DebitCardPaymentResult.builder()
            .debitCardId(CARD_ID)
            .maskedCardNumber("************7890")
            .totalAmount(new BigDecimal("100.00"))
            .totalCommission(BigDecimal.ZERO)
            .debitedAccounts(List.of(DebitedAccountEntry.builder()
                    .accountId("acc-1")
                    .maskedAccountNumber("************0-01")
                    .debitedAmount(new BigDecimal("100.00"))
                    .appliedCommission(BigDecimal.ZERO)
                    .previousBalance(new BigDecimal("350.00"))
                    .newBalance(new BigDecimal("250.00"))
                    .build()))
            .processedAt(CREATED_AT)
            .build();
  }
}
