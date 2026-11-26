package com.pe.pivot.account.application.usecase.debitcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DeleteDebitCardUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to close a debit card")
class DeleteDebitCardUseCaseImplTest {

  private static final String CARD_ID = "dc-1";

  @Mock
  private DebitCardRepositoryPort debitCardRepository;

  private DeleteDebitCardUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new DeleteDebitCardUseCaseImpl(debitCardRepository);
  }

  @Test
  @DisplayName("Should be closed logically when the debit card is active")
  void shouldBeClosedLogicallyWhenDebitCardIsActive() {
    // (happy path)
    // Arrange
    DebitCard card = DebitCard.builder()
            .id(CARD_ID)
            .status(DebitCardStatus.ACTIVE)
            .build();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(debitCardRepository.save(card)).thenReturn(Single.just(card));

    // Act
    TestObserver<Void> observer = useCase.delete(CARD_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertNoValues();
    assertThat(card.getStatus()).isEqualTo(DebitCardStatus.CLOSED);
    assertThat(card.getUpdatedAt()).isNotNull();
    verify(debitCardRepository).save(card);
  }

  @Test
  @DisplayName("Should be rejected when the debit card does not exist")
  void shouldBeRejectedWhenDebitCardDoesNotExist() {
    // (error: debit card not found)
    // Arrange
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<Void> observer = useCase.delete(CARD_ID).test();

    // Assert
    observer.assertError(DebitCardNotFoundException.class);
    observer.assertNotComplete();
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when the debit card is already CLOSED")
  void shouldBeRejectedWhenDebitCardIsAlreadyClosed() {
    // (error: already closed debit card)
    // Arrange
    DebitCard card = DebitCard.builder()
            .id(CARD_ID)
            .status(DebitCardStatus.CLOSED)
            .build();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));

    // Act
    TestObserver<Void> observer = useCase.delete(CARD_ID).test();

    // Assert
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("already CLOSED"));
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be propagated when the logical deletion fails")
  void shouldBePropagatedWhenLogicalDeletionFails() {
    // (error: persistence failure)
    // Arrange
    DebitCard card = DebitCard.builder()
            .id(CARD_ID)
            .status(DebitCardStatus.ACTIVE)
            .build();
    when(debitCardRepository.findById(CARD_ID)).thenReturn(Maybe.just(card));
    when(debitCardRepository.save(card))
            .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<Void> observer = useCase.delete(CARD_ID).test();

    // Assert
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
  }
}
