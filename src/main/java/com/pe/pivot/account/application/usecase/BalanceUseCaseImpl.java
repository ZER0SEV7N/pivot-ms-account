package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.application.support.initializers.AccountBalancePolicy;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.in.UpdateBalanceUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility Use Case orchestrating reactive account balance updates.
 * Delegates movement policies and arithmetic strategies to AccountBalancePolicy and OperationType.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceUseCaseImpl implements UpdateBalanceUseCase {

  private final AccountRepositoryPort accountRepository;
  private final FindAccountByIdUseCase findAccountByIdUseCase;
  private final AccountBalancePolicy balancePolicy;

  /**
   * Updates the balance of a bank account based on the specified operation type and amount.
   *
   * @param id - The unique identifier of the bank account to update.
   * @param operationType - The type of operation to perform (e.g., DEPOSIT, WITHDRAWAL).
   * @param amount - The amount to be added or subtracted from the account balance.
   * @return - Single emitting the updated BankAccount after the balance operation is applied.
   */
  @Override
  public Single<BankAccount> updateBalance(String id, OperationType operationType,
      BigDecimal amount) {
    log.debug("Processing balance operation for accountId: {}, operation: {}, amount: {}",
        id, operationType, amount);

    // Retrieve the account by ID, apply the balance operation, and save the updated account.
    return findAccountByIdUseCase.findById(id)
        .flatMap(account -> {
          balancePolicy.applyMovement(account, operationType, amount);
          return accountRepository.save(account);
        })

        // Log the successful balance update with relevant details for auditing and monitoring.
        .doOnSuccess(updated -> log.info(
            "Balance updated for accountId: {}. New Balance: {}, AppliedCommission: {}, "
                + "Movements: {}",
            updated.getId(), updated.getBalance(), updated.getLastAppliedCommission(),
            updated.getCurrentMonthlyMovements()));
  }
}
