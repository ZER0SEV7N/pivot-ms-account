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
   * Determines whether the personal strategy supports the given client type.
   *
   * @param customerType - The type of the customer (e.g., "PERSONAL", "BUSINESS").
   * @return - true if this strategy supports the given customer type, false otherwise.
   */
  @Override
  public boolean supports(String customerType) {
    return "PERSONAL".equals(customerType);
  }

  /**
   * Validates the new account against customer rules and existing customer accounts.
   *
   * @param customer - The customer summary information.
   * @param newAccount - account being created
   * @param existingAccounts - list of accounts alredy owned by the customer
   * @param hasActiveCreditCard - indicates if the customer has an active credit card
   * @return - Completable completing if valid or emitting BusinessRuleException
   */
  @Override
  public Completable validate(CustomerSummary customer, BankAccount newAccount,
      List<BankAccount> existingAccounts, boolean hasActiveCreditCard) {

    // Validate that the customer does not already own
    // an active account of the same type (SAVINGS or CHECKING)
    return Completable.fromAction(() -> {
      validateSingleAccountOwnership(newAccount.getAccountType(), existingAccounts);
      validateVipProfileRequirements(customer, newAccount, hasActiveCreditCard);
    });
  }

  /**
   * Validates that the customer does not already own an active account of the same type (SAVINGS
   * or CHECKING).
   *
   * @param newType - The type of the new account being created.
   * @param existingAccounts - List of existing accounts owned by the customer.
   * @throws BusinessRuleException if the customer already owns an active account of the same type.
   */
  private void validateSingleAccountOwnership(AccountType newType,
      List<BankAccount> existingAccounts) {
    // Personal customers can only own a maximum of one active SAVINGS or CHECKING account.
    if (newType == AccountType.SAVINGS || newType == AccountType.CHECKING) {
      boolean alreadyOwnsType = existingAccounts.stream()
          .anyMatch(acc -> acc.getAccountType() == newType
              && acc.getStatus() != AccountStatus.CLOSED);
      // If the customer already owns an active account of the same type, throw an exception.
      if (alreadyOwnsType) {
        throw new BusinessRuleException(
            "Personal customers can only own a maximum of one active " + newType + " account.");
      }
    }
  }

  /**
   * Validates that VIP Personal customers must hold an active Credit Card with the bank to open a
   * VIP SAVINGS account.
   *
   * @param customer - The customer summary information.
   * @param newAccount - account being created
   * @param hasActiveCreditCard - indicates if the customer has an active credit card
   * @throws BusinessRuleException if the VIP Personal customer does not hold an active Credit Card
   *     when trying to open a VIP SAVINGS account.
   */
  private void validateVipProfileRequirements(CustomerSummary customer, BankAccount newAccount,
      boolean hasActiveCreditCard) {
    // If the customer has a VIP profile and is trying to open a SAVINGS account,
    if (customer.isVip() && newAccount.getAccountType() == AccountType.SAVINGS
        && !hasActiveCreditCard) {
      throw new BusinessRuleException(
          "VIP Personal customers must hold an active Credit Card with the bank to open a VIP "
              + "SAVINGS account.");
    }
  }
}
