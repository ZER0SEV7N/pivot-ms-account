package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.*;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.strategy.AccountValidationStrategy;
import com.pe.pivot.account.infrastructure.config.AccountRulesProperties;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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
        UpdateBalanceUseCase,
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
    public Single<BankAccount> execute(BankAccount account) {
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
     */
    @Override
    public Single<BankAccount> findByAccountNumber(String accountNumber) {
        String hash = cryptoUtil.hashCode(accountNumber);
        return accountRepository.findByAccountNumber(hash)
                .switchIfEmpty(Single.error(new AccountNotFoundException("Bank account not found with account number: " + accountNumber)));
    }

}
