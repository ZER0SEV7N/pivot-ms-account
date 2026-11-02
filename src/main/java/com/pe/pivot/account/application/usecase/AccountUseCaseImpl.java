package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.exception.DuplicateAccountException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.port.in.*;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.strategy.AccountValidationStrategy;
import com.pe.pivot.account.infrastructure.config.AccountRulesProperties;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Application Service implementing all passive bank account use cases with RxJava 3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountUseCaseImpl implements
        CreateAccountUseCase,
        RetrieveAccountUseCase,
        UpdateAccountUseCase,
        DeleteAccountUseCase {

    private final AccountRepositoryPort accountRepository;
    private final CustomerClientPort customerClient;
    private final List<AccountValidationStrategy> validationStrategies;
    private final AccountRulesProperties rulesProperties;
    private final CryptoUtil cryptoUtil;


    /**
     * Opens a new bank account after verifying the customer in ms-customer and evaluating strategies.
     *
     * @param account domain account to create
     * @return Single emitting the created BankAccount
     */
    @Override
    public Single<BankAccount> create(BankAccount account) {
        // Log the initiation of the account opening process
        log.debug("Initiating account opening for customerId: {} and type: {}", account.getCustomerId(), account.getAccountType());

        // Verify the customer exists and is active in ms-customer
        return customerClient.findCustomerById(account.getCustomerId())
                .switchIfEmpty(Single.error(new CustomerNotFoundException("Customer not found in ms-customer with ID: " + account.getCustomerId())))
                .flatMap(customer -> {
                    if (!customer.isActive()) {
                        return Single.error(new BusinessRuleException(
                                "Cannot open a bank account for an inactive or blocked customer."));
                    }

                    // Retrieve existing accounts for the customer to apply validation strategies
                    return accountRepository.findByCustomerId(customer.getId())
                            .toList()
                            .flatMap(existingAccounts -> applyStrategy(customer, account, existingAccounts)
                                    .andThen(Single.defer(() -> {
                                        configureProductDefaults(account, customer);
                                        return verifyUniquenessAndSave(account);
                                    })));
                })
                .doOnSuccess(saved -> log.info("Bank account created successfully. ID: {}, MaskedNumber: {}",
                        saved.getId(),
                        DataMaskingUtil.maskAccountNumber(saved.getAccountNumber())));
    }

    /**
     * Retrieves all bank accounts from the repository.
     *
     * @return - Flowable emitting all BankAccount instances
     */
    @Override
    public Flowable<BankAccount> findAll() {
        log.debug("Retrieving all bank accounts");
        return accountRepository.findAll();
    }

    /**
     * Finds a Bank account by its mongo Id
     *
     * @param id - the unique identifier of the bank account
     * @return - Single emitting the found BankAccount or an error if not found
     */
    @Override
    public Single<BankAccount> findById(String id) {
        log.debug("Searching bank account by ID: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Single.error(new AccountNotFoundException("Bank account not found with ID: " + id)));
    }

    /**
     * Finds a bank account using the SHA-256 blind index hash of the account number.
     *
     * @param accountNumber - the plain account number to search for
     * @return - Single emitting the found BankAccount or an error if not found
     */
    @Override
    public Single<BankAccount> findByAccountNumber(String accountNumber) {
        String hash = cryptoUtil.hashSha256(accountNumber);
        log.debug("Searching bank account by masked number: {}", DataMaskingUtil.maskAccountNumber(accountNumber));
        return accountRepository.findByAccountNumberHash(hash)
                .switchIfEmpty(Single.error(new AccountNotFoundException(
                        "Bank account not found with number: " + DataMaskingUtil.maskAccountNumber(accountNumber))));
    }

    /**
     * Retrieves all bank accounts belonging to a specific customer ID.
     *
     * @param customerId - the unique identifier of the customer
     * @return - Flowable emitting all BankAccount instances for the customer
     * @throws CustomerNotFoundException if the customer does not exist in ms-customer
     * @throws BusinessRuleException if the customer is inactive or blocked
     */
    @Override
    public Flowable<BankAccount> findByCustomerId(String customerId) {
        log.debug("Retrieving bank accounts for customerId: {}", customerId);
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Updates the details of an existing bank account after validating the input and applying business rules.
     *
     * @param id - the unique identifier of the bank account to update
     * @param update - the BankAccount object containing updated fields
     * @return - Single emitting the updated BankAccount or an error if not found or validation fails
     */
    @Override
    public Single<BankAccount> updateAccount(String id, BankAccount update ) {
        log.debug("Updating bank account with ID: {}", id);
        return findById(id)
                .flatMap(existing -> {
                    updateAccountFields(existing, update);
                    return accountRepository.save(existing);
                }).doOnSuccess(updated -> log.info("Bank account updated successfully with ID: {}", updated.getId()));
    }

    /**
     * Deletes a bank account by its ID.
     *
     * @param id - the unique identifier of the bank account to delete
     * @return - Completable indicating completion or error if the account does not exist
     */
    @Override
    public Completable delete(String id) {
        log.debug("Deleting bank account with ID: {}", id);
        return findById(id)
                .flatMapCompletable(existing -> accountRepository.deleteById(existing.getId()))
                .doOnComplete(() -> log.info("Bank account deleted successfully with ID: {}", id));
    }

    /**
     * Applies the validation strategies to the new account and existing accounts.
     *
     * @param customer - the customer summary
     * @param newAccount - the new bank account to validate
     * @param existingAccounts - the list of existing bank accounts
     * @return - Completable indicating completion or error if validation fails
     */
    private Completable applyStrategy(CustomerSummary customer, BankAccount newAccount, List<BankAccount> existingAccounts) {
        return validationStrategies.stream()
                .filter(strategy -> strategy.supports(customer.getCustomerType()))
                .findFirst()
                .map(strategy -> strategy.validate(customer, newAccount, existingAccounts))
                .orElseGet(() -> Completable.error(new BusinessRuleException("Unsupported customer type: " + customer.getCustomerType())));
    }

    /**
     * Verifies the uniqueness of the account number and saves the bank account if unique.
     *
     * @param account - the bank account to verify and save
     * @return - Single emitting the saved BankAccount or an error if a duplicate exists
     */
    private Single<BankAccount> verifyUniquenessAndSave(BankAccount account) {
        return accountRepository.existsByAccountNumberHash(account.getAccountNumberHash())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Single.error(new DuplicateAccountException(
                                "A bank account with the specified account number already exists."));
                    }
                    return accountRepository.save(account);
                });
    }

    /**
     * Configures default values for a new bank account based on its type and the associated customer information.
     *
     * @param account - the bank account to configure
     * @param customer - the customer summary associated with the account
     * @throws BusinessRuleException - if any business rule is violated during configuration
     */
    private void configureProductDefaults(BankAccount account, CustomerSummary customer) {
        // Set default values for account fields based on account type and customer information
        String rawAccountNumber = Optional.ofNullable(account.getAccountNumber())
                .filter(num -> !num.isBlank())
                .orElseGet(() -> "191-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-0-01");

        // Set account number, hash, currency, balance, status, and other defaults
        account.setAccountNumber(rawAccountNumber);
        account.setAccountNumberHash(cryptoUtil.hashSha256(rawAccountNumber));
        account.setCurrency(Optional.ofNullable(account.getCurrency()).orElse("PEN"));
        account.setBalance(Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO));
        account.setCurrentMonthlyMovements(0);
        account.setStatus(Optional.ofNullable(account.getStatus()).orElse(AccountStatus.ACTIVE));

        // Set account type specific defaults
        if (account.getAccountType() == AccountType.SAVINGS) {
            account.setMaintenanceFee(rulesProperties.getZeroMaintenanceFee());
            account.setMaxMonthlyMovements(Optional.ofNullable(account.getMaxMonthlyMovements())
                    .orElse(rulesProperties.getSavingsMaxMonthlyMovements()));
            account.setAllowedMovementDay(null);

        // Set defaults for CHECKING and FIXED_TERM account types
        } else if (account.getAccountType() == AccountType.CHECKING) {
            account.setMaintenanceFee(Optional.ofNullable(account.getMaintenanceFee())
                    .orElse(rulesProperties.getCheckingMaintenanceFee()));
            account.setMaxMonthlyMovements(null);
            account.setAllowedMovementDay(null);

        // Set defaults for FIXED_TERM account type
        } else if (account.getAccountType() == AccountType.FIXED_TERM) {
            account.setMaintenanceFee(rulesProperties.getZeroMaintenanceFee());
            account.setMaxMonthlyMovements(rulesProperties.getFixedTermMaxMonthlyMovements());
        }

        // Set holders and authorized signers based on customer information
        if (account.getHolders() == null || account.getHolders().isEmpty()) {
            account.setHolders(List.of(customer.getDocumentNumber()));
        }

        // Set authorized signers to an empty list if not provided
        if (account.getAuthorizedSigners() == null) {
            account.setAuthorizedSigners(Collections.emptyList());
        }

        // Set the creation and update timestamps to the current instant
        Instant now = Instant.now();
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
    }

    /**
     *  Updates the fields of an existing bank account with values from the incoming account, preserving non-null values.
     *
     * @param existing - the existing bank account to update
     * @param incoming - the incoming bank account containing updated values
     */
    private void updateAccountFields(BankAccount existing, BankAccount incoming) {
        // Update only non-null fields from the incoming account to the existing account
        Optional.ofNullable(incoming.getCurrency()).ifPresent(existing::setCurrency);
        Optional.ofNullable(incoming.getBalance()).ifPresent(existing::setBalance);
        Optional.ofNullable(incoming.getMaintenanceFee()).ifPresent(existing::setMaintenanceFee);
        Optional.ofNullable(incoming.getMaxMonthlyMovements()).ifPresent(existing::setMaxMonthlyMovements);
        Optional.ofNullable(incoming.getAllowedMovementDay()).ifPresent(existing::setAllowedMovementDay);
        Optional.ofNullable(incoming.getHolders()).ifPresent(existing::setHolders);
        Optional.ofNullable(incoming.getAuthorizedSigners()).ifPresent(existing::setAuthorizedSigners);
        Optional.ofNullable(incoming.getStatus()).ifPresent(existing::setStatus);
        existing.setUpdatedAt(Instant.now());
    }

}
