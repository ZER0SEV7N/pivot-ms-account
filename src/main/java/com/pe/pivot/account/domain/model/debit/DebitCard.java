package com.pe.pivot.account.domain.model.debit;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Core Domain Model representing a Debit Card linked to a primary bank account
 * and an ordered list of secondary bank accounts for cascading payments.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitCard {

  private String id;
  private String cardNumber;
  private String cardNumberHash;
  private String customerId;
  private String customerName;
  private String primaryAccountId;
  private List<String> secondaryAccountIds;
  private DebitCardStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  /**
   * Checks whether this debit card is currently ACTIVE.
   *
   * @return true if status is ACTIVE
   */
  public boolean isActive() {
    return this.status == DebitCardStatus.ACTIVE;
  }

  /**
   * Returns all associated account IDs in strict priority order: primary account first,
   * followed by unique secondary accounts.
   *
   * @return ordered list of linked accounts IDs
   */
  public List<String> getOrderedAccountIds() {
    Set<String> ordered = new LinkedHashSet<>();

    // Add secondary accounts first to maintain their order and uniqueness
    if (this.primaryAccountId != null && !this.primaryAccountId.isBlank()) {
      ordered.add(this.primaryAccountId);
    }

    // Add secondary accounts in the order they were added, ensuring uniqueness
    if (this.secondaryAccountIds != null) {
      ordered.addAll(this.secondaryAccountIds);
    }

    // Return an unmodifiable list to prevent external modifications to the internal state
    return List.copyOf(ordered);
  }
}
