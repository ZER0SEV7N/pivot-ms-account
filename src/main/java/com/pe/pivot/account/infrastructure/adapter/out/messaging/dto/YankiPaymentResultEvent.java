package com.pe.pivot.account.infrastructure.adapter.out.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * Kafka event payload published to yanki.payment.result for ms-yanki and ms-transaction.
 */
@Builder
public record YankiPaymentResultEvent(
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