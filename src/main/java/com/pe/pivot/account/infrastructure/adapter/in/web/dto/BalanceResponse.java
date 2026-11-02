package com.pe.pivot.account.infrastructure.adapter.in.web.dto;

import com.pe.pivot.account.domain.model.AccountType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable Response DTO using Java 17 Record for consulting available account balance.
 */
@Builder
public record BalanceResponse(
        String accountId,
        String accountNumber,
        AccountType accountType,
        String currency,
        BigDecimal availableBalance,
        Integer currentMonthlyMovements,
        Integer maxMonthlyMovements,
        Instant queriedAt
) {
}