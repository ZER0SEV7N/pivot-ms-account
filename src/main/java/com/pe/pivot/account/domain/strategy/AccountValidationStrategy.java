package com.pe.pivot.account.domain.strategy;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Completable;

import java.util.List;

/**
 * Strategy interface for validating bank account creation rules based on customer type.
 * Applies the Open/Closed Principle (SOLID).
 */
public interface AccountValidationStrategy {

    /**
     * Determines if this strategy applies o the given customer type.
     *
     * @param customerType - The type of the customer (e.g., "PERSONAL", "BUSINESS").
     * @return true if supported
     */
    boolean supports(String customerType);

    /**
     * Validates the new account against customer rules and existing customer accounts.
     *
     * @param customer - The customer summary information.
     * @param newAccount - account being created
     * @param existingAccounts - list of accounts alredy owned by the customer
     * @return Completable completing if valid or emitting BusinessRuleException
     */
    Completable validate(CustomerSummary customer, BankAccount newAccount, List<BankAccount> existingAccounts);
}
