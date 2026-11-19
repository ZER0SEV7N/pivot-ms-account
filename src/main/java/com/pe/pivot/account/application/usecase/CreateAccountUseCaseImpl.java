package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.application.support.accounts.AccountDomainInitializer;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.exception.DuplicateAccountException;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.port.in.CreateAccountUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.CreditClientPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.strategy.AccountValidationStrategy;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility Use Case for creating passive bank accounts with Phase II VIP/PYME rules.
 * It ensures that the customer is active, validates account creation rules based on customer type,
 * and initializes the account with default parameters, cryptographic blind-index hashes, and
 * product-type financial rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccountUseCaseImpl implements CreateAccountUseCase {

  private final AccountRepositoryPort accountRepository;
  private final CustomerClientPort customerClient;
  private final CreditClientPort creditClient;
  private final List<AccountValidationStrategy> validationStrategies;
  private final AccountDomainInitializer accountInitializer;

  /**
   * Creates a new bank account after validating customer status, overdue debt, and product rules.
   *
   * @param account the bank account to be created
   * @return Single emitting the created BankAccount or an error if validation fails
   */
  @Override
  public Single<BankAccount> create(BankAccount account) {
    log.debug("Initiating account creation for customerId: {} and type: {}",
            account.getCustomerId(), account.getAccountType());

    // return if the customer is active, has no overdue debt, and passes all strategy validations
    return fetchActiveCustomer(account.getCustomerId())
            .flatMap(customer -> validateNoOverdueDebt(customer)
                    // Check if the customer has an active credit card if required by VIP or PYME profiles
                    .andThen(Single.defer(() -> Single.zip(
                            accountRepository.findByCustomerId(customer.getId()).toList(),
                            checkCreditCardIfRequired(customer),
                            (existingAccounts, hasCard) -> validateWithStrategy(
                                    customer, account, existingAccounts, hasCard)
                                    .andThen(Single.defer(() -> {
                                      accountInitializer.initializeAndValidate(customer, account);
                                      return saveUniqueAccount(account);
                                    }))
                    ).flatMap(single -> single)))
            )
            .doOnSuccess(saved -> log.info("Account created successfully. ID: {}, MaskedNumber: {}",
                    saved.getId(), DataMaskingUtil.maskAccountNumber(saved.getAccountNumber()))
            );
  }

  /**
   * Fetches the customer summary from ms-customer and verifies that the customer is ACTIVE.
   *
   * @param customerId the unique identifier of the customer
   * @return Single emitting the active CustomerSummary
   */
  private Single<CustomerSummary> fetchActiveCustomer(String customerId) {
    // Fetch the customer from ms-customer and verify that they are active
    return customerClient.findCustomerById(customerId)
            .switchIfEmpty(Single.error(new CustomerNotFoundException(
                    "Customer not found in ms-customer with ID: " + customerId)))
            // Validate that the customer is active before proceeding with account creation
            .flatMap(customer -> customer.isActive()
                    ? Single.just(customer)
                    : Single.error(new BusinessRuleException(
                    "Cannot open a bank account for an inactive or blocked customer.")));
  }

  /**
   * Verifies that the customer does not hold any overdue credit debt in ms-credit.
   *
   * @param customer the customer summary
   * @return Completable completing if eligible, or emitting BusinessRuleException if overdue
   */
  private Completable validateNoOverdueDebt(CustomerSummary customer) {
    // Check if the customer has any overdue credit debt in ms-credit before allowing account creation
    return creditClient.hasOverdueDebt(customer.getId())
            .flatMapCompletable(hasOverdue -> Boolean.TRUE.equals(hasOverdue)
                    ? Completable.error(new BusinessRuleException(
                    "Customer cannot open a new bank account while holding an overdue credit debt."))
                    : Completable.complete());
  }

  /**
   * Checks if the customer has an active credit card when required by VIP or PYME profiles.
   *
   * @param customer the customer summary information
   * @return Single emitting true if an active credit card exists
   */
  private Single<Boolean> checkCreditCardIfRequired(CustomerSummary customer) {
    // For VIP or PYME customers, check if they have an active credit card in ms-credit.
    return (customer.isVip() || customer.isPyme())
            ? creditClient.hasActiveCreditCard(customer.getId())
            : Single.just(false);
  }

  /**
   * Validates the new account against customer rules using the appropriate strategy.
   *
   * @param customer the customer summary information
   * @param account the bank account to be created
   * @param existing existing accounts owned by the customer
   * @param hasCard whether the customer holds an active credit card
   * @return Completable completing if valid
   */
  private Completable validateWithStrategy(CustomerSummary customer, BankAccount account,
                                           List<BankAccount> existing, boolean hasCard) {
    // Find the appropriate validation strategy based on the customer type and apply it to validate the new account.
    return validationStrategies.stream()
            .filter(strategy -> strategy.supports(customer.getCustomerType()))
            .findFirst()
            .map(strategy -> strategy.validate(customer, account, existing, hasCard))
            .orElseGet(() -> Completable.error(new BusinessRuleException(
                    "Unsupported customer type: " + customer.getCustomerType())));
  }

  /**
   * Persists the bank account after verifying its blind-index hash is unique.
   *
   * @param account the bank account to persist
   * @return Single emitting the saved BankAccount
   */
  private Single<BankAccount> saveUniqueAccount(BankAccount account) {
    // Check if an account with the same blind-index hash already exists to prevent duplicates
    return accountRepository.existsByAccountNumberHash(account.getAccountNumberHash())
            .flatMap(exists -> Boolean.TRUE.equals(exists)
                    ? Single.error(new DuplicateAccountException(
                    "An account with the specified account number already exists."))
                    : accountRepository.save(account));
  }
}