package com.pe.pivot.account.infrastructure.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
    private Integer maxMonthlyMovements;
    private Integer currentMonthlyMovements;
    private Integer allowedMovementDay;
    private List<String> holders;
    private List<String> authorizedSigners;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}

