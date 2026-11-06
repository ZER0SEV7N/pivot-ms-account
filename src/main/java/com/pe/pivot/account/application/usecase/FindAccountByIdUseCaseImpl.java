package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application Service responsible only for finding a bank account by ID.
 * It centralizes the repository lookup and not-found behavior for all
 * application services that need an existing account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindAccountByIdUseCaseImpl implements FindAccountByIdUseCase {

    private final AccountRepositoryPort accountRepository;

    /**
     * Finds a Bank account by its mongo Id.
     *
     * @param id the unique identifier of the bank account
     * @return Single emitting the found BankAccount or an error if not found
     */
    @Override
    public Single<BankAccount> findById(String id) {
        log.debug("Searching bank account by ID: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Single.error(new AccountNotFoundException("Bank account not found with ID: " + id)));
    }
}
