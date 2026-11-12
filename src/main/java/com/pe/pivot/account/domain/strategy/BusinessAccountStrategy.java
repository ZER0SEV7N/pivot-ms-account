package com.pe.pivot.account.domain.strategy;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Completable;

import java.math.BigDecimal;
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
     * Validates the new account against customer rules and existing customer accounts.
     *
     * @param customer - The customer summary information.
     * @param newAccount - account being created
     * @param existingAccounts - list of accounts alredy owned by the customer
     * @param hasActiveCreditCard - indicates if the customer has an active credit card
     * @return - Completable completing if valid or emitting BusinessRuleException
     * @throws - BusinessRuleException if the customer violates any business rules for account creation.
     */
    @Override
    public Completable validate(
            CustomerSummary customer,
            BankAccount newAccount,
            List<BankAccount> existingAccounts,
            boolean hasActiveCreditCard) {
        // Validate that the customer does not already own
        return Completable.fromAction(() -> {
            // Validate that the customer does not already own
            // an active account of the same type (SAVINGS or FIXED_TERM)
            if (newAccount.getAccountType() == AccountType.SAVINGS || newAccount.getAccountType() == AccountType.FIXED_TERM) {
                throw new BusinessRuleException("Business customers are not allowed to open SAVINGS or FIXED_TERM accounts. Only CHECKING accounts are permitted.");
            }

            // Validate that the new account has at least one holder for CHECKING accounts
            if (newAccount.getHolders() == null || newAccount.getHolders().isEmpty()) {
                throw new BusinessRuleException("Business CHECKING accounts must have at least one account holder.");
            }

            // Validate that the new account has zero or more authorized signers for CHECKING accounts
            validatePymeProfileRequirements(customer, newAccount, hasActiveCreditCard);
        });
    }

    /**
     * Validates that PYME Business customers must hold an
     * active Credit Card with the bank to open a commission-free PYME CHECKING account.
     *
     * @param customer - The customer summary information.
     * @param newAccount - account being created
     * @param hasActiveCreditCard - indicates if the customer has an active credit card
     * @throws BusinessRuleException if the PYME Business customer does not
     */
    private void validatePymeProfileRequirements(
            CustomerSummary customer, BankAccount newAccount, boolean hasActiveCreditCard) {
        // If the customer has a PYME profile and is trying to open a CHECKING account,
        if (customer.isPyme()) {

            // If the customer has a PYME profile and is trying to open a CHECKING account,
            // they must have an active credit card to open a commission-free PYME CHECKING account.
            if (!hasActiveCreditCard) {
                throw new BusinessRuleException("PYME Business customers must hold an active Credit Card with the bank to open a commission-free PYME CHECKING account.");
            }
            newAccount.setMaintenanceFee(BigDecimal.ZERO);
        }
    }
}
