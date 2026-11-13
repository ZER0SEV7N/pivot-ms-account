package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Domain Policy encapsulating movement eligibility rules, automatic commission calculation,
 * and state mutation for account balance operations.
 */
@Component
public class AccountBalancePolicy {

  /**
   * Applies a movement (deposit or withdrawal) to the given bank account, updating its balance,
   *
   * @param account - the bank account to which the movement will be applied
   * @param operationType - the type of operation (DEPOSIT or WITHDRAWAL)
   * @param amount - the amount to deposit or withdraw
   * @throws BusinessRuleException if any precondition fails (e.g., account not ACTIVE
   */
  public void applyMovement(BankAccount account, OperationType operationType, BigDecimal amount) {
    // Validate preconditions before applying the movement
    validatePreconditions(account, amount);

    // Calculate the new balance after applying the movement and any applicable commission
    BigDecimal currentBalance = Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO);
    BigDecimal commission = resolveCommission(account);
    BigDecimal newBalance = operationType.calculateNewBalance(currentBalance, amount, commission);

    // Update the account's balance, movement count, and last applied commission
    int currentMovements = Optional.ofNullable(account.getCurrentMonthlyMovements()).orElse(0);
    account.setBalance(newBalance);
    account.setCurrentMonthlyMovements(currentMovements + 1);
    account.setLastAppliedCommission(commission);
    account.setUpdatedAt(Instant.now());
  }

  /**
   * Validates the preconditions for applying a movement to the bank account, including account status,
   * transaction amount, and any applicable account-specific limits.
   *
   * @param account - the bank account to validate
   * @param amount - the amount to deposit or withdraw
   */
  private void validatePreconditions(BankAccount account, BigDecimal amount) {
    // Validate that the account is ACTIVE before allowing any balance operations.
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Account is not ACTIVE. Current status: " + account.getStatus());
    }

    // Validate that the transaction amount is positive and greater than zero.
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException("Transaction amount must be greater than zero.");
    }

    // Validate that the account has not exceeded its maximum monthly movements limit.
    validateMonthlyLimit(account);
    validateFixedTermWindow(account);
  }

  /**
   * Validates that the bank account has not exceeded its maximum monthly movements limit.
   *
   * @param account - the bank account to validate
   * @throws BusinessRuleException if the account has reached its maximum monthly movements limit
   */
  private void validateMonthlyLimit(BankAccount account) {
    // Validate that the account has not exceeded its maximum monthly movements limit.
    int currentMovements = Optional.ofNullable(account.getCurrentMonthlyMovements()).orElse(0);
    if (account.getMaxMonthlyMovements() != null && currentMovements >= account.getMaxMonthlyMovements()) {
      throw new BusinessRuleException("Maximum monthly movements limit (" + account.getMaxMonthlyMovements() + ") reached for this account.");
    }
  }

  /**
   * Validates that the bank account is within its allowed movement window for fixed-term accounts.
   *
   * @param account - the bank account to validate
   * @throws BusinessRuleException if the account is not within its allowed movement window
   */
  private void validateFixedTermWindow(BankAccount account) {
    if (account.getAccountType() == AccountType.FIXED_TERM && account.getAllowedMovementDay() != null) {
      int today = LocalDate.now(ZoneOffset.UTC).getDayOfMonth();
      if (today != account.getAllowedMovementDay()) {
        throw new BusinessRuleException("FIXED_TERM accounts only allow movements on day " + account.getAllowedMovementDay() + " of the month.");
      }
    }
  }

  /**
   * Resolves the commission fee for the given bank account based on its transaction eligibility.
   *
   * @param account - the bank account for which to resolve the commission fee
   * @return - the commission fee to be applied for the transaction, or zero if the account has free transactions available
   */
  private BigDecimal resolveCommission(BankAccount account) {
    return account.isFreeTransactionAvailable()
            ? BigDecimal.ZERO
            : Optional.ofNullable(account.getTransactionCommissionFee()).orElse(BigDecimal.ZERO);
  }
}