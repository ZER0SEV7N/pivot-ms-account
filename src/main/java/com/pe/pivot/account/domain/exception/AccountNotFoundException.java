package com.pe.pivot.account.domain.exception;

/**
 * Exception thrown when a requested bank account is not found.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
