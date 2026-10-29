package com.pe.pivot.account.domain.exception;

/**
 * Exception thrown when the customer referenced in an account request does not exist in ms-customer.
 */
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
