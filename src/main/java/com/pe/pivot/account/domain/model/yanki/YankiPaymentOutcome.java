package com.pe.pivot.account.domain.model.yanki;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * Domain outcome produced after processing a Yanki payment against primary debit card accounts.
 */
@Builder
public record YankiPaymentOutcome(
        String transactionId,
        String status,
        String senderPhoneNumber,
        String receiverPhoneNumber,
        String senderDebitCardId,
        String receiverDebitCardId,
        String debitedAccountId,
        String creditedAccountId,
        String currency,
        BigDecimal amount,
        String failureReason,
        Instant processedAt
) {
}