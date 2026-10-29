package com.pe.pivot.account.domain.strategy;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Completable;

import java.util.List;

/**
 * Validation strategy for BUSINESS customers opening bank accounts.
 * Rule: Cannot have SAVINGS or FIXED_TERM accounts. Can have multiple CHECKING accounts
 * with at least one holder and zero or more authorized signers.
 */
public class BusinessAccountStrategy implements AccountValidationStrategy {

    /**
     * Determines if this strategy supports the given customer type.
     *
     * @param customerType - The type of the customer (e.g., "PERSONAL", "BUSINESS").
     * @return - true if this strategy supports the given customer type, false otherwise.
     */
    @Override
    public boolean supports(String customerType) {
        return "BUSINESS".equalsIgnoreCase(customerType);
    }

    /**
     * Validates the new account against business customer rules and existing customer accounts.
     *
     * @param customer - The customer summary information.
     * @param newAccount - account being created
     * @param existingAccounts - list of accounts alredy owned by the customer
     * @return - Completable completing if valid or emitting BusinessRuleException
     */
    @Override
    public Completable validate(CustomerSummary customer, BankAccount newAccount, List<BankAccount> existingAccounts) {
        return Completable.fromAction(() -> {

            // Validate that the customer is business and active
            if (!"BUSINESS".equals(customer.getCustomerType()) || !customer.isActive()) {
                throw new BusinessRuleException("Only active business customers can open new accounts.");
            }

            // Validate account type rules for business customers:
            // Business customers can only open CHECKING accounts. SAVINGS and FIXED_TERM accounts are not allowed.
            if (newAccount.getAccountType() == AccountType.SAVINGS
                    || newAccount.getAccountType() == AccountType.FIXED_TERM) {
                throw new BusinessRuleException("Business customers are not allowed to open SAVINGS or FIXED_TERM accounts. Only CHECKING accounts are permitted.");
            }

            if (newAccount.getHolders() == null || newAccount.getHolders().isEmpty()) {
                throw new BusinessRuleException("Business bank accounts must have at least one account holder.");
            }
        });
    }
}
