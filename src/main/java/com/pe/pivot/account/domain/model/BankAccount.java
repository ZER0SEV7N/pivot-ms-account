package com.pe.pivot.account.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Core Domain Model representing a Passive Bank Account.
 * Free of framework and persistence annotations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {

  private String id;
  private String accountNumber;
  private String accountNumberHash;
  private String customerId;
  private AccountType accountType;
  private String currency;
  private BigDecimal balance;
  private BigDecimal maintenanceFee;
  private BigDecimal minimumDailyAverage;
  private BigDecimal minOpeningAmount;
  private Integer maxMonthlyMovements;
  private Integer maxFreeMonthlyTransactions;
  private BigDecimal transactionCommissionFee;
  private BigDecimal lastAppliedCommission;
  private Integer currentMonthlyMovements;
  private Integer allowedMovementDay;
  private List<String> holders;
  private List<String> authorizedSigners;
  private AccountStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  public boolean canBeClosed() {
    return this.balance == null || this.balance.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * Checks if the bank account has free transactions available for the current month.
   *
   * @return - true if free transactions are available, false otherwise.
   */
  public boolean isFreeTransactionAvailable() {
    if (this.maxFreeMonthlyTransactions == null) {
      return true;
    }
    int current = this.currentMonthlyMovements != null ? this.currentMonthlyMovements : 0;
    return current < this.maxFreeMonthlyTransactions;
  }
}
