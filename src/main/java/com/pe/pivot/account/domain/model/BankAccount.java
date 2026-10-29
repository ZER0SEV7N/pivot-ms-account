package com.pe.pivot.account.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
    private Integer maxMonthlyMovements;
    private Integer currentMonthlyMovements;
    private Integer allowedMovementDay;
    private List<String> holders;
    private List<String> authorizedSigners;
    private Integer freeTransactionsLimit;
    private BigDecimal extraTransactionFee;
    private BigDecimal minimumDailyAverage;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
