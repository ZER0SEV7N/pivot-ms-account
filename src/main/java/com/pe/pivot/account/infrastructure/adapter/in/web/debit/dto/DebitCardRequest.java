package com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;

/**
 * Request payload for issuing a new DebitCard.
 */
@Builder
public record DebitCardRequest(
        @NotBlank(message = "customerId is required") String customerId,
        @NotBlank(message = "primaryAccountId is required") String primaryAccountId,
        List<String> secondaryAccountIds
) {
}