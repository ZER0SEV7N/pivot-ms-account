package com.pe.pivot.account.infrastructure.adapter.in.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * Kafka event payload consumed from yanki.payment.requested.
 */
@Builder
public record YankiPaymentRequestedEvent(
        String transactionId,
        String senderPhoneNumber,
        String receiverPhoneNumber,
        String senderDebitCardId,
        String receiverDebitCardId,
        String currency,
        BigDecimal amount,
        String description,
        Instant requestedAt
) {
}