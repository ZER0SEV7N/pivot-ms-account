package com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto;

import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import java.util.List;
import lombok.Builder;

/**
 * Request payload for updating linked accounts or status of an existing DebitCard.
 */
@Builder
public record DebitCardUpdateRequest(
        String primaryAccountId,
        List<String> secondaryAccountIds,
        DebitCardStatus status
) {
}