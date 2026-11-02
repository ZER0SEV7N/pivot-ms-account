// src/main/java/com/pe/pivot/account/application/usecase/BalanceUseCaseImpl.java
package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.port.in.UpdateBalanceUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Application Service strictly responsible for processing balance updates (deposits and withdrawals)
 * and enforcing movement limits (SRP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceUseCaseImpl implements UpdateBalanceUseCase {

    private final AccountRepositoryPort accountRepository;

    /**
     * Updates the balance of a bank account based on the operation type (deposit or withdrawal).
     *
     * @param id - the unique identifier of the bank account
     * @param operationType - the type of operation (DEPOSIT or WITHDRAWAL)
     * @param amount - the amount to deposit or withdraw
     * @return - Single emitting the updated BankAccount
     */
    @Override
    public Single<BankAccount> updateBalance(String id, OperationType operationType, BigDecimal amount) {
        log.debug("Processing balance update for accountId: {}, operation: {}, amount: {}", id, operationType, amount);

        return accountRepository.findById(id)
                .switchIfEmpty(Single.error(new AccountNotFoundException("Bank account not found with ID: " + id)))
                .flatMap(account -> {
                    // Validate account status
                    if (account.getStatus() != AccountStatus.ACTIVE) {
                        return Single.error(new BusinessRuleException("Bank account is not ACTIVE."));
                    }

                    // Validate that the transaction amount is greater than zero
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        return Single.error(new BusinessRuleException("Transaction amount must be greater than zero."));
                    }

                    // Validate fixed-term specific day of month rule
                    if (account.getAccountType() == AccountType.FIXED_TERM) {
                        int todayDay = LocalDate.now(ZoneOffset.UTC).getDayOfMonth();
                        if (account.getAllowedMovementDay() != null && todayDay != account.getAllowedMovementDay()) {
                            return Single.error(new BusinessRuleException(
                                    "FIXED_TERM account only allows movements on day "
                                            + account.getAllowedMovementDay() + " of the month."));
                        }
                    }

                    // Validate max monthly movements limit (for SAVINGS and FIXED_TERM)
                    int currentMovements = Optional.ofNullable(account.getCurrentMonthlyMovements()).orElse(0);
                    if (account.getMaxMonthlyMovements() != null
                            && currentMovements >= account.getMaxMonthlyMovements()) {
                        return Single.error(new BusinessRuleException(
                                "Maximum monthly movements limit (" + account.getMaxMonthlyMovements()
                                        + ") reached for this account."));
                    }

                    // Calculate new balance based on operation type
                    BigDecimal currentBalance = Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO);
                    BigDecimal newBalance;
                    if (operationType == OperationType.WITHDRAWAL) {
                        if (currentBalance.compareTo(amount) < 0) {
                            return Single.error(new BusinessRuleException(
                                    "Insufficient funds in bank account for withdrawal."));
                        }
                        newBalance = currentBalance.subtract(amount);
                    } else {
                        newBalance = currentBalance.add(amount);
                    }

                    // Update account balance and increment current monthly movements
                    account.setBalance(newBalance);
                    account.setCurrentMonthlyMovements(currentMovements + 1);
                    account.setUpdatedAt(Instant.now());

                    // Persist the updated account and return the result
                    return accountRepository.save(account);
                })
                .doOnSuccess(updated -> log.info("Balance updated for accountId: {}. New balance: {}, Movements: {}",
                        updated.getId(), updated.getBalance(), updated.getCurrentMonthlyMovements()));
    }
}