package com.pe.pivot.account.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain value object holding customer information retrieved from ms-customer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummary {

  private String id;
  private String customerType;
  private String profile;
  private String documentNumber;
  private String status;
  private String firstName;
  private String lastName;
  private String businessName;

  /**
   * Checks if the customer is a personal customer.
   *
   * @return - true if the customer is personal, false otherwise.
   */
  public boolean isPersonal() {
    return "PERSONAL".equalsIgnoreCase(this.customerType);
  }

  /**
   * Checks if the customer is a business customer.
   *
   * @return - true if the customer is business, false otherwise.
   */
  public boolean isBusiness() {
    return "BUSINESS".equalsIgnoreCase(this.customerType);
  }

  /**
   * Checks if the customer is active.
   *
   * @return - true if the customer is active, false otherwise.
   */
  public boolean isActive() {
    return "ACTIVE".equalsIgnoreCase(this.status);
  }

  /**
   * Checks if the customer has a VIP profile.
   *
   * @return - true if the customer has a VIP profile, false otherwise.
   */
  public boolean isVip() {
    return "VIP".equalsIgnoreCase(this.profile);
  }

  /**
   * Checks if the customer has a PYME profile.
   *
   * @return - true if the customer has a PYME profile, false otherwise.
   */
  public boolean isPyme() {
    return "PYME".equalsIgnoreCase(this.profile);
  }

  /**
   * Returns a human-readable display name (business name or personal full name).
   *
   * @return formatted customer display name
   */
  public String getDisplayName() {
    // If the customer is a business and has a non-empty business name, return it.
    if (isBusiness() && this.businessName != null && !this.businessName.isBlank()) {
      return this.businessName.trim();
    }

    // If the customer is personal, return the concatenation of first and last names.
    String first = this.firstName != null ? this.firstName.trim() : "";
    String last = this.lastName != null ? this.lastName.trim() : "";
    return (first + " " + last).trim();
  }
}
