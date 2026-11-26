package com.pe.pivot.account.infrastructure.adapter.in.web.account.dto;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

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

        @PositiveOrZero(message = "Initial balance must be zero or positive")
        BigDecimal balance,

        @PositiveOrZero(message = "Maintenance fee must be zero or positive")
        BigDecimal maintenanceFee,

        @PositiveOrZero(message = "Minimum daily average must be zero or positive")
        BigDecimal minimumDailyAverage,

        @PositiveOrZero(message = "Minimum opening amount must be zero or positive")
        BigDecimal minOpeningAmount,

        @Positive(message = "Max monthly movements must be at least 1")
        Integer maxMonthlyMovements,

        @PositiveOrZero(message = "Max free monthly transactions must be zero or positive")
        Integer maxFreeMonthlyTransactions,

        @PositiveOrZero(message = "Transaction commission fee must be zero or positive")
        BigDecimal transactionCommissionFee,

        @Min(value = 1, message = "Allowed movement day must be between 1 and 31")
        @Max(value = 31, message = "Allowed movement day must be between 1 and 31")
        Integer allowedMovementDay,

        List<String> holders,
        List<String> authorizedSigners,
        AccountStatus status
) {
}
