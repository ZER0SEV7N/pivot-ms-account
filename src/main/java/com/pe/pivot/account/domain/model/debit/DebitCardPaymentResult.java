package com.pe.pivot.account.domain.model.debit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Immutable domain result of a cascading debit card payment operation.
 */
@Builder
public record DebitCardPaymentResult(
        String debitCardId,
        String maskedCardNumber,
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        List<DebitedAccountEntry> debitedAccounts,
        Instant processedAt
) {
}