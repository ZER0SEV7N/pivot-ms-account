package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.exception.DuplicateAccountException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.port.in.CreateAccountUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.strategy.AccountValidationStrategy;
import com.pe.pivot.account.infrastructure.config.AccountRulesProperties;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import io.reactivex.rxjava3.core.Completable;
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
 * Application Service responsible only for opening new bank accounts.
 * It verifies the customer, applies account validation strategies, configures
 * product defaults, checks account-number uniqueness, and persists the account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccountUseCaseImpl implements CreateAccountUseCase {

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
        log.debug("Initiating account opening for customerId: {} and type: {}",
                account.getCustomerId(), account.getAccountType());

        // Verify the customer exists and is active in ms-customer
        return customerClient.findCustomerById(account.getCustomerId())
                .switchIfEmpty(Single.error(new CustomerNotFoundException(
                        "Customer not found in ms-customer with ID: " + account.getCustomerId())))
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
                .doOnSuccess(saved -> log.info(
                        "Bank account created successfully. ID: {}, MaskedNumber: {}",
                        saved.getId(), DataMaskingUtil.maskAccountNumber(saved.getAccountNumber())));
    }

    /**
     * Applies the validation strategies to the new account and existing accounts.
     *
     * @param customer the customer summary
     * @param newAccount the new bank account to validate
     * @param existingAccounts the list of existing bank accounts
     * @return Completable indicating completion or error if validation fails
     */
    private Completable applyStrategy(CustomerSummary customer, BankAccount newAccount,
                                      List<BankAccount> existingAccounts) {
        return validationStrategies.stream()
                .filter(strategy -> strategy.supports(customer.getCustomerType()))
                .findFirst()
                .map(strategy -> strategy.validate(customer, newAccount, existingAccounts))
                .orElseGet(() -> Completable.error(new BusinessRuleException(
                        "Unsupported customer type: " + customer.getCustomerType())));
    }

    /**
     * Verifies the uniqueness of the account number and saves the bank account if unique.
     *
     * @param account the bank account to verify and save
     * @return Single emitting the saved BankAccount or an error if a duplicate exists
     */
    private Single<BankAccount> verifyUniquenessAndSave(BankAccount account) {
        return accountRepository.existsByAccountNumberHash(account.getAccountNumberHash())
                .flatMap(exists -> {
                    // Guard condition: If a bank account with the same account number already exists,
                    // throw a DuplicateAccountException
                    if (Boolean.TRUE.equals(exists)) {
                        return Single.error(new DuplicateAccountException(
                                "A bank account with the specified account number already exists."));
                    }
                    return accountRepository.save(account);
                });
    }

    /**
     * Configures default values for a new bank account based on its type and
     * the associated customer information.
     *
     * @param account the bank account to configure
     * @param customer the customer summary associated with the account
     */
    private void configureProductDefaults(BankAccount account, CustomerSummary customer) {
        // Set default values for account fields based on account type and customer information
        String rawAccountNumber = Optional.ofNullable(account.getAccountNumber())
                .filter(num -> !num.isBlank())
                .orElseGet(() -> "191-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                        + "-0-01");

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
}
