package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.DeleteAccountUseCase;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Application Service responsible only for the soft deletion of bank accounts.
 * It closes an account after ensuring it is not already closed and has no
 * remaining balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteAccountUseCaseImpl implements DeleteAccountUseCase {
    private final AccountRepositoryPort accountRepository;
    private final FindAccountByIdUseCase findAccountByIdUseCase;

    /**
     * Logically closes (Soft Delete) a bank account by setting its status to CLOSED.
     * Enforces the guard condition: [balance == 0].
     *
     * @param id the unique identifier of the bank account to close
     * @return Completable indicating completion or error if balance > 0
     */
    @Override
    public Completable delete(String id) {
        log.debug("Initiating soft delete (logical closure) for bank account ID: {}", id);
        return findAccountByIdUseCase.findById(id)
                .flatMapCompletable(existing -> {
                    if (existing.getStatus() == AccountStatus.CLOSED) {
                        return Completable.error(new BusinessRuleException(
                                "Bank account is already CLOSED."));
                    }
                    BigDecimal currentBalance = Optional.ofNullable(existing.getBalance())
                            .orElse(BigDecimal.ZERO);
                    if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                        return Completable.error(new BusinessRuleException(
                                "Cannot close a bank account with remaining balance (" + currentBalance
                                        + "). Please withdraw all funds first."));
                    }
                    existing.setStatus(AccountStatus.CLOSED);
                    existing.setUpdatedAt(Instant.now());
                    return accountRepository.save(existing).ignoreElement();
                })
                .doOnComplete(() -> log.info(
                        "Bank account logically closed (CLOSED) with ID: {}", id));
    }
}
