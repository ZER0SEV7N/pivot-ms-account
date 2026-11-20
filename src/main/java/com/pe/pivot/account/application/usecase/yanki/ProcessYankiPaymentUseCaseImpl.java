package com.pe.pivot.account.application.usecase.yanki;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.yanki.YankiPaymentCommand;
import com.pe.pivot.account.domain.model.yanki.YankiPaymentOutcome;
import com.pe.pivot.account.domain.port.in.UpdateBalanceUseCase;
import com.pe.pivot.account.domain.port.in.yanki.ProcessYankiPaymentUseCase;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * Use Case processing Yanki payment events exclusively against the primary account
 * linked to each participant's debit card, with compensating rollback on partial failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessYankiPaymentUseCaseImpl implements ProcessYankiPaymentUseCase {

  private static final String STATUS_COMPLETED = "COMPLETED";
  private static final String STATUS_FAILED = "FAILED";

  private final DebitCardRepositoryPort debitCardRepository;
  private final UpdateBalanceUseCase updateBalanceUseCase;

  /**
   * Processes a Yanki payment command by debiting the sender's account and crediting the
   * receiver's account.
   *
   * @param cmd the YankiPaymentCommand containing the payment details
   * @return a Single emitting the YankiPaymentOutcome indicating the result of the payment
   *     processing
   */
  @Override
  public Single<YankiPaymentOutcome> process(YankiPaymentCommand cmd) {
    log.info("Processing Yanki payment event. TxId: {}, Amount: {}",
            cmd.transactionId(), cmd.amount());

    // Debit the sender's account if a sender debit card is provided, otherwise skip to crediting
    // the receiver
    return debitSenderIfLinked(cmd)
            .flatMap(debitedAccountId -> creditReceiverWithCompensation(cmd, debitedAccountId)
                    .map(creditedAccountId -> buildOutcome(
                            cmd, STATUS_COMPLETED, blankToNull(debitedAccountId),
                            blankToNull(creditedAccountId), null)))
            .onErrorReturn(ex -> {
              log.warn(
                      "Yanki payment failed for TxId {}: {}",
                      cmd.transactionId(), ex.getMessage());
              return buildOutcome(cmd, STATUS_FAILED, null, null, ex.getMessage());
            });
  }

  /**
   * Debits the sender's account if a sender debit card ID is provided in the command.
   *
   * @param cmd the YankiPaymentCommand containing the payment details
   * @return a Single emitting the debited account ID if successful, or an empty string if no
   *     sender debit card is provided
   */
  private Single<String> debitSenderIfLinked(@NonNull YankiPaymentCommand cmd) {
    // If no sender debit card ID is provided, return an empty string to indicate no debit
    // operation is needed
    if (cmd.senderDebitCardId() == null || cmd.senderDebitCardId().isBlank()) {
      return Single.just("");
    }
    // Resolve the sender's debit card and perform a withdrawal operation on the linked primary
    // account
    return resolveActiveCard(cmd.senderDebitCardId())
            .flatMap(card -> updateBalanceUseCase
                    .updateBalance(
                            card.getPrimaryAccountId(), OperationType.WITHDRAWAL, cmd.amount())
                    .map(updated -> card.getPrimaryAccountId()));
  }

  /**
   * Credits the receiver's account with compensation logic in case of failure.
   *
   * @param cmd the YankiPaymentCommand containing the payment details
   * @param debitedAccountId the account ID that was debited from the sender, used for
   *     compensation if needed
   * @return a Single emitting the credited account ID if successful, or an error if the credit
   *     operation fails
   */
  private Single<String> creditReceiverWithCompensation(
          @NonNull YankiPaymentCommand cmd, String debitedAccountId) {
    // If no receiver debit card ID is provided, return an empty string to indicate no credit
    // operation is needed
    if (cmd.receiverDebitCardId() == null || cmd.receiverDebitCardId().isBlank()) {
      return Single.just("");
    }

    // Resolve the receiver's debit card and perform a deposit operation on the linked primary
    // account
    return resolveActiveCard(cmd.receiverDebitCardId())
            // If the credit operation fails, attempt to compensate by refunding the sender's
            // account if it was debited
            .flatMap(card -> updateBalanceUseCase
                    .updateBalance(card.getPrimaryAccountId(), OperationType.DEPOSIT, cmd.amount())
                    .map(updated -> card.getPrimaryAccountId()))
            .onErrorResumeNext(creditError -> {
              if (debitedAccountId == null || debitedAccountId.isBlank()) {
                return Single.error(creditError);
              }
              log.warn("Compensating sender debit on account {} for Yanki TxId {}",
                      debitedAccountId, cmd.transactionId());
              return updateBalanceUseCase
                      .updateBalance(debitedAccountId, OperationType.DEPOSIT, cmd.amount())
                      .flatMap(ignored -> Single.error(creditError));
            });
  }

  /**
   * Resolves a debit card by its ID and ensures it is ACTIVE.
   *
   * @param debitCardId the unique identifier of the debit card to resolve
   * @return a Single emitting the DebitCard if found and ACTIVE, or an error if not found or not
   *     ACTIVE
   * @throws DebitCardNotFoundException if the debit card with the given ID does not exist
   * @throws BusinessRuleException if the debit card is not ACTIVE
   */
  private Single<DebitCard> resolveActiveCard(String debitCardId) {
    return debitCardRepository.findById(debitCardId)
            .switchIfEmpty(Single.error(
                    new DebitCardNotFoundException("Debit card not found with ID: " + debitCardId)))
            .flatMap(card -> card.isActive()
                    ? Single.just(card)
                    : Single.error(new BusinessRuleException(
                    "Debit card " + debitCardId + " is not ACTIVE.")));
  }

  /**
   * Builds a YankiPaymentOutcome object based on the provided parameters.
   *
   * @param cmd the YankiPaymentCommand containing the payment details
   * @param status the status of the payment processing (COMPLETED or FAILED)
   * @param debitedAccountId the account ID that was debited from the sender, or null if no debit
   *     occurred
   * @param creditedAccountId the account ID that was credited to the receiver, or null if no
   *     credit occurred
   * @param failureReason the reason for failure if the payment processing failed, or null if
   *     successful
   * @return a YankiPaymentOutcome object representing the result of the payment processing
   */
  private YankiPaymentOutcome buildOutcome(YankiPaymentCommand cmd, String status,
                                           String debitedAccountId, String creditedAccountId,
                                           String failureReason) {
    return YankiPaymentOutcome.builder()
            .transactionId(cmd.transactionId())
            .status(status)
            .senderPhoneNumber(cmd.senderPhoneNumber())
            .receiverPhoneNumber(cmd.receiverPhoneNumber())
            .senderDebitCardId(cmd.senderDebitCardId())
            .receiverDebitCardId(cmd.receiverDebitCardId())
            .debitedAccountId(debitedAccountId)
            .creditedAccountId(creditedAccountId)
            .currency(cmd.currency())
            .amount(cmd.amount())
            .failureReason(failureReason)
            .processedAt(Instant.now())
            .build();
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}