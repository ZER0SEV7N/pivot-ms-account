package com.pe.pivot.account.infrastructure.adapter.out.client.credit.dto;

import lombok.Builder;

/**
 * Immutable DTO representing a Credit product summary returned by ms-credit (:8083).
 */
@Builder
public record CreditSummaryDto(
        String id,
        String customerId,
        String creditType,
        String status
) {

  /**
   * Checks whether this credit summary corresponds to an active credit card.
   *
   * @return true if the credit is an active personal or business credit card, false otherwise.
   */
  public boolean isActiveCreditCard() {
    return "ACTIVE".equalsIgnoreCase(this.status)
        && ("PERSONAL_CREDIT_CARD".equalsIgnoreCase(this.creditType)
            || "BUSINESS_CREDIT_CARD".equalsIgnoreCase(this.creditType));
  }
}
