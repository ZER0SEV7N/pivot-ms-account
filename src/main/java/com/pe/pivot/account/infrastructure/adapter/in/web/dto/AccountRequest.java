package com.pe.pivot.account.infrastructure.adapter.in.web.dto;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable Request DTO using Java 17 Record for creating or updating a bank account (Part I).
 */
@Builder
public record AccountRequest(
        String accountNumber,

        @NotBlank(message = "customerId is required")
        String customerId,

        @NotNull(message = "accountType is required")
        AccountType accountType,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "balance is required")
        @PositiveOrZero(message = "balance must be zero or positive")
        BigDecimal balance,

        @PositiveOrZero(message = "maintenanceFee must be zero or positive")
        BigDecimal maintenanceFee,

        @Min(value = 1, message = "maxMonthlyMovements must be at least 1")
        Integer maxMonthlyMovements,

        @Min(value = 1, message = "allowedMovementDay must be between 1 and 31")
        @Max(value = 31, message = "allowedMovementDay must be between 1 and 31")
        Integer allowedMovementDay,

        List<String> holders,
        List<String> authorizedSigners,
        AccountStatus status
) {
}