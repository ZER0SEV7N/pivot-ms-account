package com.pe.pivot.account.application.usecase.debitcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RetrieveDebitCardUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to retrieve debit cards")
class RetrieveDebitCardUseCaseImplTest {

  private static final String CARD_ID = "dc-1";
  private static final String CUSTOMER_ID = "cust-1";
  private static final String PRIMARY_ACCOUNT = "acc-1";
  private static final String CARD_NUMBER = "4557891234567890";
  private static final String CARD_HASH = "hash-value";

  @Mock
  private DebitCardRepositoryPort debitCardRepository;

  @Mock
  private FindAccountByIdUseCase findAccountByIdUseCase;

  @Mock
  private CryptoUtil cryptoUtil;

  private RetrieveDebitCardUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new RetrieveDebitCardUseCaseImpl(debitCardRepository, findAccountByIdUseCase,
            cryptoUtil);
  }

  @Test
  @DisplayName("Should be returned when the debit card id exists")
  void shouldBeReturnedWhenDebitCardIdExists() {
    // (happy path)
    // Arrange
    DebitCard card = card();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));

    // Act
    TestObserver<DebitCard> observer = useCase.findById(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(debitCardRepository).findById(CARD_ID);
  }

  @Test
  @DisplayName("Should be delegated to the repository when retrieving every debit card")
  void shouldBeDelegatedToRepositoryWhenRetrievingEveryDebitCard() {
    // Arrange
    DebitCard first = card();
    DebitCard second = DebitCard.builder().id("dc-2").build();
    when(debitCardRepository.findAll()).thenReturn(Flowable.just(first, second));

    // Act
    TestSubscriber<DebitCard> observer = useCase.findAll().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(2);
    observer.assertValues(first, second);
    verify(debitCardRepository).findAll();
  }

  @Test
  @DisplayName("Should be rejected when the debit card id does not exist")
  void shouldBeRejectedWhenDebitCardIdDoesNotExist() {
    // (error: debit card not found)
    // Arrange
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<DebitCard> observer = useCase.findById(CARD_ID).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(DebitCardNotFoundException.class);
    observer.assertNotComplete();
  }

  @Test
  @DisplayName("Should be found through the blind index hash when the card number exists")
  void shouldBeFoundThroughBlindIndexHashWhenCardNumberExists() {
    // Arrange
    DebitCard card = card();
    when(cryptoUtil.hashSha256(CARD_NUMBER)).thenReturn(CARD_HASH);
    when(debitCardRepository.findByCardNumberHash(CARD_HASH)).thenReturn(Maybe.just(card));

    // Act
    TestObserver<DebitCard> observer = useCase.findByCardNumber(CARD_NUMBER).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(cryptoUtil).hashSha256(CARD_NUMBER);
    verify(debitCardRepository).findByCardNumberHash(CARD_HASH);
  }

  @Test
  @DisplayName("Should be rejected when the card number is unknown")
  void shouldBeRejectedWhenCardNumberIsUnknown() {
    // (error: card number not found)
    // Arrange
    when(cryptoUtil.hashSha256(CARD_NUMBER)).thenReturn(CARD_HASH);
    when(debitCardRepository.findByCardNumberHash(CARD_HASH)).thenReturn(Maybe.empty());

    // Act
    TestObserver<DebitCard> observer = useCase.findByCardNumber(CARD_NUMBER).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(DebitCardNotFoundException.class);
    observer.assertNotComplete();
  }

  @Test
  @DisplayName("Should be delegated to the repository when searching by customer id")
  void shouldBeDelegatedToRepositoryWhenSearchingByCustomerId() {
    // Arrange
    DebitCard card = card();
    when(debitCardRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Flowable.just(card));

    // Act
    TestSubscriber<DebitCard> observer = useCase.findByCustomerId(CUSTOMER_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(card);
    verify(debitCardRepository).findByCustomerId(CUSTOMER_ID);
  }

  @Test
  @DisplayName("Should be resolved to the primary account when consulting the balance")
  void shouldBeResolvedToPrimaryAccountWhenConsultingBalance() {
    // Arrange
    BankAccount account = BankAccount.builder()
            .id(PRIMARY_ACCOUNT)
            .accountNumber("191-00000000-0-01")
            .accountType(AccountType.SAVINGS)
            .status(AccountStatus.ACTIVE)
            .balance(new BigDecimal("250.00"))
            .build();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card()));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT)).thenReturn(Single.just(account));

    // Act
    TestObserver<BankAccount> observer = useCase.getPrimaryAccountBalance(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    assertThat(observer.values().get(0).getBalance()).isEqualByComparingTo("250.00");
    verify(findAccountByIdUseCase).findById(PRIMARY_ACCOUNT);
  }

  @Test
  @DisplayName("Should be rejected when the debit card of the balance query does not exist")
  void shouldBeRejectedWhenDebitCardOfBalanceQueryDoesNotExist() {
    // (error: debit card not found)
    // Arrange
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<BankAccount> observer = useCase.getPrimaryAccountBalance(CARD_ID).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(DebitCardNotFoundException.class);
    verifyNoInteractions(findAccountByIdUseCase);
  }

  private DebitCard card() {
    return DebitCard.builder()
            .id(CARD_ID)
            .cardNumber(CARD_NUMBER)
            .customerId(CUSTOMER_ID)
            .primaryAccountId(PRIMARY_ACCOUNT)
            .status(DebitCardStatus.ACTIVE)
            .build();
  }
}
