package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.in.RetrieveAccountUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application Service responsible only for retrieving bank accounts.
 * It provides reactive queries for all accounts, account IDs, account numbers,
 * and customer IDs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveAccountUseCaseImpl implements RetrieveAccountUseCase {
    private final AccountRepositoryPort accountRepository;
    private final FindAccountByIdUseCase findAccountByIdUseCase;
    private final CryptoUtil cryptoUtil;

    /**
     * Retrieves all bank accounts from the repository.
     *
     * @return Flowable emitting all BankAccount instances
     */
    @Override
    public Flowable<BankAccount> findAll() {
        log.debug("Retrieving all bank accounts");
        return accountRepository.findAll();
    }

    /**
     * Finds a Bank account by its mongo Id.
     *
     * @param id the unique identifier of the bank account
     * @return Single emitting the found BankAccount or an error if not found
     */
    @Override
    public Single<BankAccount> findById(String id) {
        return findAccountByIdUseCase.findById(id);
    }

    /**
     * Finds a bank account using the SHA-256 blind index hash of the account number.
     *
     * @param accountNumber the plain account number to search for
     * @return Single emitting the found BankAccount or an error if not found
     */
    @Override
    public Single<BankAccount> findByAccountNumber(String accountNumber) {
        String hash = cryptoUtil.hashSha256(accountNumber);
        log.debug("Searching bank account by masked number: {}",
                DataMaskingUtil.maskAccountNumber(accountNumber));
        return accountRepository.findByAccountNumberHash(hash)
                .switchIfEmpty(Single.error(new AccountNotFoundException("Bank account not found with number: " + DataMaskingUtil.maskAccountNumber(accountNumber))));
    }

    /**
     * Retrieves all bank accounts belonging to a specific customer ID.
     *
     * @param customerId the unique identifier of the customer
     * @return Flowable emitting all BankAccount instances for the customer
     */
    @Override
    public Flowable<BankAccount> findByCustomerId(String customerId) {
        log.debug("Retrieving bank accounts for customerId: {}", customerId);
        return accountRepository.findByCustomerId(customerId);
    }
}
