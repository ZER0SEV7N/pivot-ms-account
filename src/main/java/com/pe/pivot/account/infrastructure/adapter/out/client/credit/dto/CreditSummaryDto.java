package com.pe.pivot.account.infrastructure.adapter.out.client.credit.dto;

import java.math.BigDecimal;
import lombok.Builder;

/**
 * Immutable DTO representing a Credit product summary returned by ms-credit (:8083).
 */
@Builder
public record CreditSummaryDto(
        String id,
        String customerId,
        String creditType,
        String status,
        BigDecimal usedAmount
) {

  // Constants for credit status and types to facilitate business logic checks.
  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_OVERDUE = "OVERDUE";
  private static final String PERSONAL_CARD = "PERSONAL_CREDIT_CARD";
  private static final String BUSINESS_CARD = "BUSINESS_CREDIT_CARD";

  /**
   * Checks whether this credit summary corresponds to an active credit card.
   *
   * @return true if the credit is an active personal or business credit card
   */
  public boolean isActiveCreditCard() {
    return STATUS_ACTIVE.equalsIgnoreCase(this.status)
            && (PERSONAL_CARD.equalsIgnoreCase(this.creditType)
            || BUSINESS_CARD.equalsIgnoreCase(this.creditType));
  }

  /**
   * Checks whether this credit summary represents an unpaid overdue debt.
   *
   * @return true if status is OVERDUE and usedAmount is greater than zero
   */
  public boolean hasOverdueDebt() {
    return STATUS_OVERDUE.equalsIgnoreCase(this.status)
            && this.usedAmount != null
            && this.usedAmount.compareTo(BigDecimal.ZERO) > 0;
  }
}
