package com.pe.pivot.account.domain.model.debit;

import java.math.BigDecimal;
import lombok.Builder;

/**
 * Immutable domain value object capturing the debit breakdown for a single account
 * participating in a debit card payment.
 */
@Builder
public record DebitedAccountEntry(
        String accountId,
        String maskedAccountNumber,
        BigDecimal debitedAmount,
        BigDecimal appliedCommission,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {
}