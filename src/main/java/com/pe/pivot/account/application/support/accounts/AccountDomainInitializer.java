package com.pe.pivot.account.application.support.accounts;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.infrastructure.config.AccountRulesProperties;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Domain Factory / Enricher responsible exclusively for initializing default parameters,
 * cryptographic blind-index hashes, and product-type financial rules on a new BankAccount.
 */
@Component
@RequiredArgsConstructor
public class AccountDomainInitializer {

  private final AccountRulesProperties rules;
  private final CryptoUtil cryptoUtil;

  /**
   * Initializes and validates a new bank account for a given customer.
   *
   * @param customer - The customer summary information.
   * @param account - The bank account to be initialized and validated.
   */
  public void initializeAndValidate(CustomerSummary customer, BankAccount account) {
    initializeBaseMetadata(customer, account);
    applyProductSpecificRules(customer, account);
    validateMinimumOpeningBalance(account);
  }

  /**
   * Initializes the base metadata for a new bank account, including account number generation.
   *
   * @param customer - The customer summary information.
   * @param account - The bank account to be initialized.
   * @throws BusinessRuleException if the initial balance is less than the required minimum opening
   *     amount
   */
  private void initializeBaseMetadata(CustomerSummary customer, BankAccount account) {
    // Set the customer ID and creation timestamp for the new account.
    String rawNumber = Optional.ofNullable(account.getAccountNumber())
        .filter(num -> !num.isBlank())
        .orElseGet(this::generateDefaultAccountNumber);

    // Set the account number and its SHA-256 hash for secure storage and indexing.
    account.setAccountNumber(rawNumber);
    account.setAccountNumberHash(cryptoUtil.hashSha256(rawNumber));
    account.setCurrency(Optional.ofNullable(account.getCurrency()).orElse("PEN"));
    account.setBalance(Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO));
    account.setCurrentMonthlyMovements(0);
    account.setLastAppliedCommission(BigDecimal.ZERO);
    account.setStatus(Optional.ofNullable(account.getStatus()).orElse(AccountStatus.ACTIVE));

    // Set the free transactions limit and commission fee from the externalized business rules.
    account.setMaxFreeMonthlyTransactions(
        Optional.ofNullable(account.getMaxFreeMonthlyTransactions())
            .orElse(rules.getMaxFreeMonthlyTransactions()));
    account.setTransactionCommissionFee(
        Optional.ofNullable(account.getTransactionCommissionFee())
            .orElse(rules.getDefaultTransactionCommission()));

    // Set account type specific defaults
    if (account.getHolders() == null || account.getHolders().isEmpty()) {
      account.setHolders(List.of(customer.getDocumentNumber()));
    }

    // Set authorized signers to an empty list if not provided
    if (account.getAuthorizedSigners() == null) {
      account.setAuthorizedSigners(Collections.emptyList());
    }

    // Set the creation and update timestamps for the new account.
    Instant now = Instant.now();
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
  }

  /**
   * Generates a default account number when none is supplied.
   *
   * @return - the generated account number
   */
  private String generateDefaultAccountNumber() {
    String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return "191-" + suffix + "-0-01";
  }

  /**
   * Applies product-specific financial rules to the new bank account based on its type and the
   * customer's profile.
   *
   * @param customer - The customer summary information.
   * @param account - The bank account to which product-specific rules will be applied.
   * @throws BusinessRuleException if the initial balance is less than the required minimum opening
   *     amount.
   */
  private void applyProductSpecificRules(CustomerSummary customer, BankAccount account) {
    // Apply product-specific financial rules based on account type and customer profile
    AccountType type = account.getAccountType();

    // Apply product-specific financial rules based on account type and customer profile
    if (type == AccountType.SAVINGS) {
      account.setMaintenanceFee(BigDecimal.ZERO);

      // Set the maximum monthly movements and minimum daily average based on customer profile
      account.setMaxMonthlyMovements(
          Optional.ofNullable(account.getMaxMonthlyMovements())
              .orElse(rules.getSavingsMaxMonthlyMovements()));

      // Set the minimum daily average and minimum opening amount based on customer profile
      if (customer.isVip()) {
        account.setMinimumDailyAverage(
            Optional.ofNullable(account.getMinimumDailyAverage())
                .orElse(rules.getVipMinimumDailyAverage()));
      } else {
        account.setMinimumDailyAverage(BigDecimal.ZERO);
      }

      // Set the minimum opening amount based on customer profile
      account.setMinOpeningAmount(
          Optional.ofNullable(account.getMinOpeningAmount())
              .orElse(customer.isVip()
                  ? rules.getVipMinOpeningAmount()
                  : rules.getDefaultMinOpeningAmount()));
      return;
    }

    // Apply product-specific financial rules based on account type and customer profile
    if (type == AccountType.CHECKING) {
      if (!customer.isPyme()) {
        account.setMaintenanceFee(
            Optional.ofNullable(account.getMaintenanceFee())
                .orElse(rules.getCheckingDefaultMaintenanceFee()));
      }

      // Set the maximum monthly movements and minimum daily average based on customer profile
      account.setMaxMonthlyMovements(null);

      // Set the minimum daily average and minimum opening amount based on customer profile
    } else if (type == AccountType.FIXED_TERM) {
      account.setMaintenanceFee(BigDecimal.ZERO);
      account.setMaxMonthlyMovements(1);
      account.setAllowedMovementDay(Optional.ofNullable(account.getAllowedMovementDay())
          .orElse(rules.getFixedTermDefaultMovementDay()));
    }

    // Set the minimum daily average and minimum opening amount based on customer profile
    account.setMinimumDailyAverage(
        Optional.ofNullable(account.getMinimumDailyAverage()).orElse(BigDecimal.ZERO));
    account.setMinOpeningAmount(Optional.ofNullable(account.getMinOpeningAmount())
        .orElse(rules.getDefaultMinOpeningAmount()));
  }

  /**
   * Validates that the initial balance of the new bank account meets the required minimum opening
   * amount.
   *
   * @param account - The bank account to be validated.
   * @throws BusinessRuleException if the initial balance is less than the required minimum opening
   *     amount
   */
  private void validateMinimumOpeningBalance(BankAccount account) {
    if (account.getBalance().compareTo(account.getMinOpeningAmount()) < 0) {
      throw new BusinessRuleException(
          "Initial balance (" + account.getBalance()
              + ") cannot be less than the required minimum opening amount ("
              + account.getMinOpeningAmount() + ").");
    }
  }
}
