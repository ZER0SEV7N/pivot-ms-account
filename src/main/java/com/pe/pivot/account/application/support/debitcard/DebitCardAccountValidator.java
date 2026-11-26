package com.pe.pivot.account.application.support.debitcard;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates that every account linked to a DebitCard exists, is ACTIVE, and belongs
 * to the cardholder (SRP, reused by Create and Update use cases).
 */
@Component
@RequiredArgsConstructor
public class DebitCardAccountValidator {

  private final FindAccountByIdUseCase findAccountByIdUseCase;

  /**
   * Validates ownership and ACTIVE status for all specified account IDs.
   *
   * @param customerId the expected owner customer ID
   * @param accountIds the list of account IDs to validate
   * @return Completable that completes if all accounts are valid, or emits BusinessRuleException
   */
  public Completable validateLinkedAccounts(String customerId, List<String> accountIds) {
    return Flowable.fromIterable(accountIds)
            .concatMapCompletable(accountId -> findAccountByIdUseCase.findById(accountId)
                    .flatMapCompletable(account -> {
                      // Validate that the account belongs to the specified customer
                      if (!customerId.equals(account.getCustomerId())) {
                        return Completable.error(new BusinessRuleException(
                                "Account ID " + accountId + " does not belong to customer ID "
                                        + customerId));
                      }

                      // Validate that the account is ACTIVE before linking it to a debit card
                      if (account.getStatus() != AccountStatus.ACTIVE) {
                        return Completable.error(new BusinessRuleException(
                                "Cannot link non-ACTIVE account " + accountId
                                        + " to a debit card."));
                      }
                      return Completable.complete();
                    })
            );
  }
}
