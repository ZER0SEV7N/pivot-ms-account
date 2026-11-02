package com.pe.pivot.account.infrastructure.adapter.in.web.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Standardized immutable Error Response DTO using Java 17 Record.
 */
@Builder
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}