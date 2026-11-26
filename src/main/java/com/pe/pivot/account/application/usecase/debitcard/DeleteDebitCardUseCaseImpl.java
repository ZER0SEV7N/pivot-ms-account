package com.pe.pivot.account.application.usecase.debitcard;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.port.in.debit.DeleteDebitCardUseCase;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use Case for logically closing (Soft Delete) a DebitCard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteDebitCardUseCaseImpl implements DeleteDebitCardUseCase {

  private final DebitCardRepositoryPort debitCardRepository;

  /**
   * Deletes a DebitCard by its unique identifier.
   * This operation performs a logical deletion by setting the status of the debit card to CLOSED.
   *
   * @param id the unique identifier of the debit card to delete
   * @return Completable that completes if the deletion is successful,
   *         or emits an error if the debit card is not found or cannot be deleted
   * @throws DebitCardNotFoundException if the debit card with the given ID does not exist
   */
  @Override
  public Completable delete(String id) {
    log.debug("Initiating soft delete for debit card ID: {}", id);

    // Check if the debit card exists and is not already CLOSED, then perform a logical deletion
    return debitCardRepository.findById(id)
            .switchIfEmpty(io.reactivex.rxjava3.core.Maybe.error(
                    new DebitCardNotFoundException("Debit card not found with ID: " + id)))
            // Validate that the debit card is not already CLOSED before proceeding with the
            // logical deletion
            .flatMapCompletable(existing -> {
              if (existing.getStatus() == DebitCardStatus.CLOSED) {
                return Completable.error(
                        new BusinessRuleException("Debit card is already CLOSED."));
              }
              // Perform logical deletion by updating the status to CLOSED and setting the updated
              // timestamp
              existing.setStatus(DebitCardStatus.CLOSED);
              existing.setUpdatedAt(Instant.now());
              return debitCardRepository.save(existing).ignoreElement();
            })
            .doOnComplete(() -> log.info("Debit card logically closed (CLOSED): {}", id));
  }
}