package com.pe.pivot.account.application.support.debitcard;

import com.pe.pivot.account.application.support.accounts.AccountBalancePolicy;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.model.debit.DebitedAccountEntry;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Domain Policy implementing Chain-of-Responsibility cascading debits across prioritized
 * bank accounts linked to a DebitCard. Guarantees atomic pre-validation before any mutation.
 */
@Component
@RequiredArgsConstructor
public class CascadingDebitPolicy {

  private final AccountBalancePolicy balancePolicy;

  /**
   * Plans and applies a cascading debit across the ordered accounts of a debit card.
   *
   * @param card the active debit card
   * @param orderedAccounts linked accounts ordered by priority (primary first)
   * @param totalAmount total payment amount to debit
   * @return CascadingExecution containing modified accounts and the payment breakdown result
   */
  public CascadingExecution execute(
          DebitCard card, List<BankAccount> orderedAccounts, BigDecimal totalAmount) {
    List<PlannedDebit> plan = buildDebitPlan(orderedAccounts, totalAmount);
    return applyPlan(card, plan, totalAmount);
  }

  /**
   * Builds a plan for the cascading debit based on the available funds in each account.
   *
   * @param accounts ordered list of accounts to debit
   * @param totalAmount total amount to be debited across accounts
   * @return list of PlannedDebit objects representing the debit plan
   * @throws BusinessRuleException if the combined spendable balance is insufficient
   */
  private List<PlannedDebit> buildDebitPlan(List<BankAccount> accounts, BigDecimal totalAmount) {
    BigDecimal remainingAmount = totalAmount;
    List<PlannedDebit> plan = new ArrayList<>();

    // Iterate through the accounts in order of priority and determine how much can be debited
    // from each
    for (BankAccount account : accounts) {
      // Skip accounts that are not ACTIVE or have no spendable balance
      if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }

      // Calculate the spendable balance for the account using the injected AccountBalancePolicy
      BigDecimal spendable = calculateSpendableBalance(account);
      if (spendable.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal toDebit = spendable.min(remainingAmount);
        plan.add(new PlannedDebit(account, toDebit));
        remainingAmount = remainingAmount.subtract(toDebit);
      }
    }

    // If after iterating through all accounts there is still a remaining amount, it means the
    // combined spendable balance is insufficient.
    if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
      throw new BusinessRuleException(
              "Insufficient combined funds across linked accounts to cover payment of "
                      + totalAmount);
    }

    return plan;
  }

  /**
   * Applies the planned debits to the accounts and constructs the result of the operation.
   *
   * @param account the bank account to calculate spendable balance for
   * @return the spendable balance of the account after considering its status and commission fees
   */
  private BigDecimal calculateSpendableBalance(BankAccount account) {
    // If the account is not ACTIVE, it cannot be used for debiting, so return zero.
    if (account.getStatus() != AccountStatus.ACTIVE) {
      return BigDecimal.ZERO;
    }

    // Calculate the spendable balance by subtracting any applicable commission fees from the
    // current balance.
    BigDecimal balance = Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO);
    BigDecimal commission = account.isFreeTransactionAvailable()
            ? BigDecimal.ZERO
            : Optional.ofNullable(account.getTransactionCommissionFee()).orElse(BigDecimal.ZERO);
    return balance.subtract(commission);
  }

  /**
   * Applies the planned debits to the accounts and constructs the result of the operation.
   *
   * @param card the debit card being used for the payment
   * @param plan the list of PlannedDebit objects representing the debit plan
   * @param totalAmount the total amount to be debited across all accounts
   * @return CascadingExecution containing the modified accounts and the payment result
   */
  private CascadingExecution applyPlan(
          DebitCard card, List<PlannedDebit> plan, BigDecimal totalAmount) {
    // Prepare lists to hold the modified accounts and the entries for the payment result
    List<BankAccount> modifiedAccounts = new ArrayList<>();
    List<DebitedAccountEntry> entries = new ArrayList<>();
    BigDecimal totalCommission = BigDecimal.ZERO;

    // Iterate through the planned debits and apply them to the respective accounts,
    // while also calculating commissions and preparing the result entries.
    for (PlannedDebit step : plan) {
      BankAccount account = step.account();
      final BigDecimal previousBalance = Optional.ofNullable(account.getBalance())
              .orElse(BigDecimal.ZERO);

      // Apply the debit operation to the account using the injected AccountBalancePolicy,
      // which handles balance updates and commission calculations.
      balancePolicy.applyMovement(account, OperationType.WITHDRAWAL, step.debitAmount());
      BigDecimal commission = Optional.ofNullable(account.getLastAppliedCommission())
              .orElse(BigDecimal.ZERO);

      // Update the total commission and prepare the entry for this account in the payment result.
      totalCommission = totalCommission.add(commission);
      modifiedAccounts.add(account);
      entries.add(toEntry(account, step.debitAmount(), commission, previousBalance));
    }

    // Construct the final payment result object capturing the overall debit operation details,
    // including the total amount, total commission, and the breakdown of debited accounts.
    DebitCardPaymentResult result = DebitCardPaymentResult.builder()
            .debitCardId(card.getId())
            .maskedCardNumber(DataMaskingUtil.maskAccountNumber(card.getCardNumber()))
            .totalAmount(totalAmount)
            .totalCommission(totalCommission)
            .debitedAccounts(entries)
            .processedAt(Instant.now())
            .build();

    return new CascadingExecution(modifiedAccounts, result);
  }

  /**
   * Converts a BankAccount and its debit operation details into a DebitedAccountEntry for the
   * payment result.
   *
   * @param account the bank account that was debited
   * @param debitedAmount the amount that was debited from the account
   * @param commission the commission applied to the debit operation
   * @param previousBalance the balance of the account before the debit operation
   * @return a DebitedAccountEntry capturing the details of the debit operation for this account
   */
  private DebitedAccountEntry toEntry(BankAccount account, BigDecimal debitedAmount,
                                      BigDecimal commission, BigDecimal previousBalance) {
    return DebitedAccountEntry.builder()
            .accountId(account.getId())
            .maskedAccountNumber(DataMaskingUtil.maskAccountNumber(account.getAccountNumber()))
            .debitedAmount(debitedAmount)
            .appliedCommission(commission)
            .previousBalance(previousBalance)
            .newBalance(account.getBalance())
            .build();
  }

  /**
   * Represents a planned debit operation for a specific bank account, including the account and
   * the amount to be debited.
   * This is an internal helper record used to facilitate the cascading debit logic.
   */
  private record PlannedDebit(BankAccount account, BigDecimal debitAmount) { }
}
