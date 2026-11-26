package com.pe.pivot.account.application.usecase.debitcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.application.support.debitcard.DebitCardAccountValidator;
import com.pe.pivot.account.application.support.debitcard.DebitCardDomainInitializer;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
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
 * Unit tests for {@link UpdateDebitCardUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to update a debit card")
class UpdateDebitCardUseCaseImplTest {

  private static final String CARD_ID = "dc-1";
  private static final String CUSTOMER_ID = "cust-1";

  @Mock
  private DebitCardRepositoryPort debitCardRepository;

  @Mock
  private DebitCardAccountValidator accountValidator;

  @Mock
  private DebitCardDomainInitializer initializer;

  private UpdateDebitCardUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new UpdateDebitCardUseCaseImpl(debitCardRepository, accountValidator, initializer);
  }

  @Test
  @DisplayName("Should be updated with the incoming accounts and status")
  void shouldBeUpdatedWithIncomingAccountsAndStatus() {
    // (happy path)
    // Arrange
    DebitCard existing = existingCard();
    DebitCard incoming = DebitCard.builder()
            .primaryAccountId("acc-9")
            .secondaryAccountIds(List.of("acc-2"))
            .status(DebitCardStatus.BLOCKED)
            .build();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(existing));
    when(initializer.sanitizeSecondaryIds("acc-9", List.of("acc-2")))
            .thenReturn(List.of("acc-2"));
    when(accountValidator.validateLinkedAccounts(CUSTOMER_ID, List.of("acc-9", "acc-2")))
            .thenReturn(Completable.complete());
    when(debitCardRepository.save(existing)).thenReturn(Single.just(existing));

    // Act
    TestObserver<DebitCard> observer = useCase.update(CARD_ID, incoming).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(existing);
    assertThat(existing.getPrimaryAccountId()).isEqualTo("acc-9");
    assertThat(existing.getSecondaryAccountIds()).containsExactly("acc-2");
    assertThat(existing.getStatus()).isEqualTo(DebitCardStatus.BLOCKED);
    assertThat(existing.getUpdatedAt()).isNotNull();
    verify(debitCardRepository).save(existing);
  }

  @Test
  @DisplayName("Should be rejected when the debit card does not exist")
  void shouldBeRejectedWhenDebitCardDoesNotExist() {
    // (error: debit card not found)
    // Arrange
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<DebitCard> observer = useCase.update(CARD_ID, DebitCard.builder().build())
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(DebitCardNotFoundException.class);
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when the debit card is already CLOSED")
  void shouldBeRejectedWhenDebitCardIsAlreadyClosed() {
    // (error: closed debit card)
    // Arrange
    DebitCard existing = existingCard();
    existing.setStatus(DebitCardStatus.CLOSED);
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(existing));

    // Act
    TestObserver<DebitCard> observer = useCase.update(CARD_ID, DebitCard.builder().build())
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("CLOSED debit card"));
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be updated keeping the existing primary and sanitizing secondaries")
  void shouldBeUpdatedKeepingExistingPrimaryAndSanitizingSecondaries() {
    // Arrange
    DebitCard existing = existingCard();
    DebitCard incoming = DebitCard.builder().build();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(existing));
    when(initializer.sanitizeSecondaryIds("acc-1", List.of("acc-2")))
            .thenReturn(List.of("acc-2"));
    when(accountValidator.validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1", "acc-2")))
            .thenReturn(Completable.complete());
    when(debitCardRepository.save(existing)).thenReturn(Single.just(existing));

    // Act
    TestObserver<DebitCard> observer = useCase.update(CARD_ID, incoming).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    assertThat(existing.getPrimaryAccountId()).isEqualTo("acc-1");
    assertThat(existing.getStatus()).isEqualTo(DebitCardStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should be propagated when the account validation fails")
  void shouldBePropagatedWhenAccountValidationFails() {
    // (error: invalid linked account)
    // Arrange
    DebitCard existing = existingCard();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(existing));
    when(initializer.sanitizeSecondaryIds("acc-1", List.of("acc-2")))
            .thenReturn(List.of("acc-2"));
    when(accountValidator.validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1", "acc-2")))
            .thenReturn(Completable.error(new BusinessRuleException("invalid account")));

    // Act
    TestObserver<DebitCard> observer = useCase.update(CARD_ID, DebitCard.builder().build())
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be propagated when the debit card persistence fails")
  void shouldBePropagatedWhenDebitCardPersistenceFails() {
    // (error: persistence failure)
    // Arrange
    DebitCard existing = existingCard();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(existing));
    when(initializer.sanitizeSecondaryIds("acc-1", List.of("acc-2")))
            .thenReturn(List.of("acc-2"));
    when(accountValidator.validateLinkedAccounts(CUSTOMER_ID, List.of("acc-1", "acc-2")))
            .thenReturn(Completable.complete());
    when(debitCardRepository.save(existing))
            .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<DebitCard> observer = useCase.update(CARD_ID, DebitCard.builder().build())
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }

  private DebitCard existingCard() {
    return DebitCard.builder()
            .id(CARD_ID)
            .customerId(CUSTOMER_ID)
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .status(DebitCardStatus.ACTIVE)
            .build();
  }
}
