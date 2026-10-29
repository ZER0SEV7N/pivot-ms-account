package com.pe.pivot.account.domain.exception;

/**
 * Exception thrown when a banking business rule or strategy validation fails.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
