package com.pe.pivot.account.domain.exception;

/**
 * Exception thrown when an account number hash already exists.
 */
public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
