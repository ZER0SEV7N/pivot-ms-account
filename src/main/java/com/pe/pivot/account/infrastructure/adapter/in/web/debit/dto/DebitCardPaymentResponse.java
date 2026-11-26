package com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Response payload summarizing a completed cascading debit card payment.
 */
@Builder
public record DebitCardPaymentResponse(
        String debitCardId,
        String maskedCardNumber,
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        List<DebitedAccountDetail> debitedAccounts,
        Instant processedAt
) {
}