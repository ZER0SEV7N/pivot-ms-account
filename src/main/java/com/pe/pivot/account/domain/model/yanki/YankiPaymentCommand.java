package com.pe.pivot.account.domain.model.yanki;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * Domain command representing a Yanki mobile wallet payment request involving debit cards.
 */
@Builder
public record YankiPaymentCommand(
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