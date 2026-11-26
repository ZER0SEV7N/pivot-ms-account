package com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto;

import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Response payload representing a DebitCard (includes readable customerName).
 */
@Builder
public record DebitCardResponse(
        String id,
        String cardNumber,
        String customerId,
        String customerName,
        String primaryAccountId,
        List<String> secondaryAccountIds,
        DebitCardStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}