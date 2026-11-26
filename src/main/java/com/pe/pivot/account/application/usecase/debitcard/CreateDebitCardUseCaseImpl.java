package com.pe.pivot.account.application.usecase.debitcard;

import com.pe.pivot.account.application.support.debitcard.DebitCardDomainInitializer;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.in.debit.CreateDebitCardUseCase;
import com.pe.pivot.account.domain.port.out.CreditClientPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use Case for issuing a new DebitCard after validating customer eligibility,
 * absence of overdue credit debt, and ownership of all linked ACTIVE accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDebitCardUseCaseImpl implements CreateDebitCardUseCase {

  private final DebitCardRepositoryPort debitCardRepository;
  private final CustomerClientPort customerClient;
  private final CreditClientPort creditClient;
  private final FindAccountByIdUseCase findAccountByIdUseCase;
  private final DebitCardDomainInitializer initializer;

  /**
   * Creates a new DebitCard in the system after performing necessary validations.
   *
   * @param debitCard the DebitCard domain model to create
   * @return Single emitting the created DebitCard or an error if validation fails
   */
  @Override
  public Single<DebitCard> create(DebitCard debitCard) {
    log.debug("Issuing debit card for customerId: {}", debitCard.getCustomerId());

    // Validate that the customer exists and is active, has no overdue credit debt,
    return fetchActiveCustomer(debitCard.getCustomerId())
            .flatMap(customer -> validateNoOverdueDebt(customer)
                    .andThen(validateLinkedAccounts(
                            customer.getId(), debitCard.getOrderedAccountIds()))
                    .andThen(Single.defer(() -> {
                      initializer.initialize(customer, debitCard);
                      return debitCardRepository.save(debitCard);
                    })))
            .doOnSuccess(saved -> log.info(
                    "Debit card issued successfully. ID: {}, MaskedNumber: {}",
                    saved.getId(), DataMaskingUtil.maskAccountNumber(saved.getCardNumber()))
            );
  }

  /**
   * Fetches the customer by ID and ensures they are active.
   *
   * @param customerId the unique identifier of the customer
   * @return Single emitting the CustomerSummary if active, or an error if not found or inactive
   */
  private Single<CustomerSummary> fetchActiveCustomer(String customerId) {
    return customerClient.findCustomerById(customerId)
            .switchIfEmpty(Single.error(new CustomerNotFoundException(
                    "Customer not found in ms-customer with ID: " + customerId)))
            .flatMap(customer -> customer.isActive()
                    ? Single.just(customer)
                    : Single.error(new BusinessRuleException(
                    "Cannot issue a debit card for an inactive or blocked customer."))
            );
  }

  /**
   * Validates that the customer has no overdue credit debt before issuing a debit card.
   *
   * @param customer the CustomerSummary to validate
   * @return Completable completing if eligible, or emitting BusinessRuleException if overdue
   */
  private Completable validateNoOverdueDebt(CustomerSummary customer) {
    return creditClient.hasOverdueDebt(customer.getId())
            .flatMapCompletable(hasOverdue -> Boolean.TRUE.equals(hasOverdue)
                    ? Completable.error(new BusinessRuleException(
                    "Customer cannot acquire a debit card while holding an overdue credit debt."))
                    : Completable.complete()
            );
  }

  /**
   * Validates that all linked accounts for the debit card belong to the customer and are ACTIVE.
   *
   * @param customerId the unique identifier of the customer
   * @param accountIds the list of account IDs to validate
   * @return Completable completing if all accounts are valid, or emitting BusinessRuleException
   *     if any account is invalid
   * @throws BusinessRuleException if any linked account does not belong to the customer or is not
   *     ACTIVE
   * @throws CustomerNotFoundException if the customer does not exist
   */
  private Completable validateLinkedAccounts(String customerId, List<String> accountIds) {
    return Flowable.fromIterable(accountIds)
            .concatMapCompletable(accountId -> findAccountByIdUseCase.findById(accountId)
                    .flatMapCompletable(account -> {
                      if (!customerId.equals(account.getCustomerId())) {
                        return Completable.error(new BusinessRuleException(
                                "Account " + accountId + " does not belong to customer "
                                        + customerId));
                      }
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
