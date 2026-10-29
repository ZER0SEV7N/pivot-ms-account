package com.pe.pivot.account.domain.model;

import lombok.*;

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

    /**
     * Checks if the customer is a personal / business and active customer.
     * @return - true if the customer is personal / business and active, false otherwise.
     */
    public boolean isPersonal() { return "PERSONAL".equalsIgnoreCase(this.customerType); }
    public boolean isBusiness() { return "BUSINESS".equalsIgnoreCase(this.customerType); }
    public boolean isActive() { return "ACTIVE".equalsIgnoreCase(this.status); }
}
