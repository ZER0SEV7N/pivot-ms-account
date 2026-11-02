package com.pe.pivot.account.infrastructure.adapter.out.client.dto;

import lombok.Builder;

/**
 * Immutable Java 17 Record representing the response payload from ms-customer.
 */
@Builder
public record CustomerClientResponse (
        String id,
        String customerType,
        String profile,
        String documentType,
        String documentNumber,
        String status
){
}
