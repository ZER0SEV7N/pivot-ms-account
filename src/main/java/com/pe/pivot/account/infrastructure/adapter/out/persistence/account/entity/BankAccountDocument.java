package com.pe.pivot.account.infrastructure.adapter.out.persistence.account.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB Document representing a passive bank account in
 * 'db_pivot_account' (Part I).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class BankAccountDocument {

  @Id
  private String id;

  private String accountNumber;

  @Indexed(unique = true)
  private String accountNumberHash;

  @Indexed
  private String customerId;

  private String accountType;
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
  private String status;
  private Instant createdAt;
  private Instant updatedAt;
}
