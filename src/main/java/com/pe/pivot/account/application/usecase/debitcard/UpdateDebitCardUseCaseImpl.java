package com.pe.pivot.account.application.usecase.debitcard;

import com.pe.pivot.account.application.support.debitcard.DebitCardAccountValidator;
import com.pe.pivot.account.application.support.debitcard.DebitCardDomainInitializer;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.port.in.debit.UpdateDebitCardUseCase;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use Case for updating linked accounts (primary/secondary order) or status of a DebitCard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateDebitCardUseCaseImpl implements UpdateDebitCardUseCase {

  private final DebitCardRepositoryPort debitCardRepository;
  private final DebitCardAccountValidator accountValidator;
  private final DebitCardDomainInitializer initializer;

  /**
   * Updates an existing debit card with new primary/secondary account IDs or status.
   *
   * @param id the unique identifier of the debit card to update
   * @param incoming the debit card to update
   * @return Single emitting the updated DebitCard, or an error if the update fails
   * @throws DebitCardNotFoundException if the debit card does not exist
   */
  @Override
  public Single<DebitCard> update(String id, DebitCard incoming) {
    log.debug("Updating debit card with ID: {}", id);

    // Validate that the debit card exists and is not CLOSED before proceeding with the update
    return debitCardRepository.findById(id)
            // If the debit card is not found, emit a DebitCardNotFoundException
            .switchIfEmpty(Single.error(
                    new DebitCardNotFoundException("Debit card not found with ID: " + id)))
            // Validate that the debit card is not CLOSED before allowing updates
            .flatMap(existing -> {
              if (existing.getStatus() == DebitCardStatus.CLOSED) {
                return Single.error(
                        new BusinessRuleException("Cannot modify a CLOSED debit card."));
              }
              // Update the primary account ID and sanitize secondary account IDs based on the incoming request
              String newPrimary = Optional.ofNullable(incoming.getPrimaryAccountId())
                      .orElse(existing.getPrimaryAccountId());
              existing.setPrimaryAccountId(newPrimary);

              // If secondary account IDs are provided in the incoming request,
              // sanitize them; otherwise, keep existing secondary account IDs
              if (incoming.getSecondaryAccountIds() != null) {
                existing.setSecondaryAccountIds(
                        initializer.sanitizeSecondaryIds(newPrimary, incoming.getSecondaryAccountIds()));
              // If secondary account IDs are not provided, keep existing secondary account IDs
              } else {
                existing.setSecondaryAccountIds(
                        initializer.sanitizeSecondaryIds(newPrimary, existing.getSecondaryAccountIds()));
              }

              // Update the status if provided in the incoming request; otherwise, keep existing status
              Optional.ofNullable(incoming.getStatus()).ifPresent(existing::setStatus);
              existing.setUpdatedAt(Instant.now());

              // Validate that all linked accounts (primary and secondary) belong to the same customer and are ACTIVE
              return accountValidator.validateLinkedAccounts(
                              existing.getCustomerId(), existing.getOrderedAccountIds())
                      .andThen(Single.defer(() -> debitCardRepository.save(existing)));
            })
            .doOnSuccess(updated -> log.info("Debit card updated successfully: {}", updated.getId()));
  }
}