package com.pe.pivot.account.domain.strategy;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Completable;

import java.util.List;

/**
 * Validation strategy for PERSONAL customer opening bank accounts.
 * RULE: Max 1 SAVINGS account, Max 1 CHECKING account,or multiple FIXED_TERM accounts.
 */
public class PersonalAccountStrategy implements AccountValidationStrategy {

    /**
     * Determines It supports the “personal strategy” client type
     *
     * @param customerType - The type of the customer (e.g., "PERSONAL", "BUSINESS").
     * @return - true if this strategy supports the given customer type, false otherwise.
     */
    @Override
    public boolean supports(String customerType) {
        return "PERSONAL".equals(customerType);
    }

    /**
     * Validates the new account against personal customer rules and existing customer accounts.
     *
     * @param customer - The customer summary information.
     * @param newAccount - account being created
     * @param existingAccounts - list of accounts alredy owned by the customer
     * @return - Completable completing if valid or emitting BusinessRuleException
     */
    @Override
    public Completable validate(CustomerSummary customer, BankAccount newAccount, List<BankAccount> existingAccounts) {
        return Completable.fromAction(() -> {

            // Validate account type rules for personal customers :
            // Personal Customer only have 1 Savings account, 1 Checking account, and multiple Fixed Term accounts.
            if (newAccount.getAccountType() == AccountType.SAVINGS) {
                boolean hasActiveSavings = existingAccounts.stream()
                        .anyMatch(acc -> acc.getAccountType() == AccountType.SAVINGS && acc.getStatus() != AccountStatus.CLOSED);
                if (hasActiveSavings) {
                    throw new BusinessRuleException("Personal customers can only own a maximum of one active SAVINGS account.");
                }
            }

            // Validate account type rules for personal customers :
            if (newAccount.getAccountType() == AccountType.CHECKING) {
                boolean hasActiveChecking = existingAccounts.stream()
                        .anyMatch(acc -> acc.getAccountType() == AccountType.CHECKING && acc.getStatus() != AccountStatus.CLOSED);
                if (hasActiveChecking) {
                    throw new BusinessRuleException("Personal customers can only own a maximum of one active CHECKING account.");
                }
            }

            // Validate allowedMovementDay for FIXED_TERM accounts
            if (newAccount.getAccountType() == AccountType.FIXED_TERM && (newAccount.getAllowedMovementDay() == null || newAccount.getAllowedMovementDay() < 1  || newAccount.getAllowedMovementDay() > 31)) {
                    throw new BusinessRuleException("FIXED_TERM accounts require a valid allowedMovementDay between 1 and 31.");
                }
        });
    }
}
