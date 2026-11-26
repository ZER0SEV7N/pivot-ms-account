package com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto;

import java.math.BigDecimal;
import lombok.Builder;

/**
 * Breakdown of a single account debited during a cascading debit card payment.
 */
@Builder
public record DebitedAccountDetail(
        String accountId,
        String maskedAccountNumber,
        BigDecimal debitedAmount,
        BigDecimal appliedCommission,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {
}