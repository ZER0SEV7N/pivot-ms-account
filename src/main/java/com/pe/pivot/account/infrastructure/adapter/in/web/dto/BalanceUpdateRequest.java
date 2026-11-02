package com.pe.pivot.account.infrastructure.adapter.in.web.dto;

import com.pe.pivot.account.domain.model.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable Request DTO using Java 17 Record for updating account balance from ms-transaction.
 */
@Builder
public record BalanceUpdateRequest(
        @NotNull(message = "operationType is required (DEPOSIT or WITHDRAWAL)")
        OperationType operationType,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be strictly positive")
        BigDecimal amount
) {
}