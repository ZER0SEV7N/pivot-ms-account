package com.pe.pivot.account.application.usecase;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Single Responsibility Use Case for creating passive bank accounts with Phase II VIP/PYME rules.
 * It ensures that the customer is active, validates account creation rules based on customer type,
 * and initializes the account with default parameters, cryptographic blind-index hashes, and product-type financial rules.
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
     * Creates a new bank account for a customer after performing all necessary validations and initializations.
     * It ensures the customer is active, validates the account creation rules, and initializes the account.
     * Use AccountDomainInitializer to set default parameters, cryptographic blind-index hashes, and product-type financial rules.
     *
     * @param account - The bank account to be created.
     * @return - Single emitting the created BankAccount or an error if validation fails.
     */
    @Override
    public Single<BankAccount> create(BankAccount account) {
        // Log the initiation of account creation for traceability and debugging.
        log.debug("Initiating account creation for customerId: {} and type: {}", account.getCustomerId(), account.getAccountType());

        // Fetch the customer summary from the ms-customer service and ensure the customer is active.
        return fetchActiveCustomer(account.getCustomerId())
                // Validate the new account against customer rules and existing accounts using the appropriate strategy.
                .flatMap(customer -> Single.zip(
                        accountRepository.findByCustomerId(customer.getId()).toList(), checkCreditCardIfRequired(customer),
                        (existingAccounts, hasCard) -> validateWithStrategy(customer, account, existingAccounts, hasCard)
                                .andThen(Single.defer(() -> {
                                    accountInitializer.initializeAndValidate(customer, account);
                                    return saveUniqueAccount(account);
                                }))
                ).flatMap(single -> single))
                .doOnSuccess(saved -> log.info("Account created successfully. ID: {}, MaskedNumber: {}",
                        saved.getId(), DataMaskingUtil.maskAccountNumber(saved.getAccountNumber())));
    }

    /**
     * Fetches the customer summary from the ms-customer service and ensures the customer is active.
     *
     * @param customerId - The unique identifier of the customer.
     * @return - Single emitting the CustomerSummary if found and active, or an error if not found or inactive.
     */
    private Single<CustomerSummary> fetchActiveCustomer(String customerId) {
        return customerClient.findCustomerById(customerId)
                .switchIfEmpty(Single.error(new CustomerNotFoundException("Customer not found in ms-customer with ID: " + customerId)))
                // Validate that the customer is active before proceeding with account creation.
                .flatMap(customer -> customer.isActive()
                        ? Single.just(customer)
                        : Single.error(new BusinessRuleException("Cannot open a bank account for an inactive or blocked customer.")));
    }

    /**
     * Checks if the customer has an active credit card if required by their profile (VIP or PYME).
     *
     * @param customer - The customer summary information.
     * @return - Single emitting true if the customer has an active credit card, false otherwise.
     */
    private Single<Boolean> checkCreditCardIfRequired(CustomerSummary customer) {
        return (customer.isVip() || customer.isPyme())
                ? creditClient.hasActiveCreditCard(customer.getId())
                : Single.just(false);
    }

    /**
     * Validates the new account against customer rules and existing customer accounts using the appropriate strategy.
     *
     * @param customer - The customer summary information.
     * @param account - The bank account to be created.
     * @param existing - List of existing accounts already owned by the customer.
     * @param hasCard - Indicates if the customer has an active credit card.
     * @return - Completable completing if valid or emitting BusinessRuleException if validation fails.
     */
    private Completable validateWithStrategy(CustomerSummary customer, BankAccount account, List<BankAccount> existing, boolean hasCard) {
        // Find the appropriate validation strategy based on the customer type and apply it to validate the new account.
        return validationStrategies.stream()
                .filter(strategy -> strategy.supports(customer.getCustomerType()))
                .findFirst()
                .map(strategy -> strategy.validate(customer, account, existing, hasCard))
                .orElseGet(() -> Completable.error(new BusinessRuleException("Unsupported customer type: " + customer.getCustomerType())));
    }

    /**
     * Saves the new bank account to the repository after ensuring that the account number is unique.
     *
     * @param account - The bank account to be saved.
     * @return - Single emitting the saved BankAccount or an error if the account number already exists.
     */
    private Single<BankAccount> saveUniqueAccount(BankAccount account) {
        return accountRepository.existsByAccountNumberHash(account.getAccountNumberHash())
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Single.error(new DuplicateAccountException("An account with the specified account number already exists."))
                        : accountRepository.save(account));
    }
}