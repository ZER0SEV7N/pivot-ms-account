package com.pe.pivot.account.domain.exception;

/**
 * Exception thrown when a requested DebitCard cannot be found.
 */
public class DebitCardNotFoundException extends RuntimeException {
  public DebitCardNotFoundException(String message) {
    super(message);
  }
}
