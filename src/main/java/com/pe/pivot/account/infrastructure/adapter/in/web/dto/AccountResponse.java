package com.pe.pivot.account.infrastructure.adapter.in.web.dto;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Immutable Response DTO representing a bank account.
 * Note: accountNumberHash is strictly internal to MongoDB and never exposed via API.
 */
@Builder
public record AccountResponse(
        String id,
        String accountNumber,
        String customerId,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        BigDecimal maintenanceFee,
        BigDecimal minimumDailyAverage,
        BigDecimal minOpeningAmount,
        Integer maxMonthlyMovements,
        Integer maxFreeMonthlyTransactions,
        BigDecimal transactionCommissionFee,
        BigDecimal lastAppliedCommission,
        Integer currentMonthlyMovements,
        Integer allowedMovementDay,
        List<String> holders,
        List<String> authorizedSigners,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
