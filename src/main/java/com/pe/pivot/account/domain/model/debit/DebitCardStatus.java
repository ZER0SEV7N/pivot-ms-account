package com.pe.pivot.account.domain.model.debit;

/**
 * Lifecycle states of a debit card. CLOSED represents a logical Soft Delete.
 */
public enum DebitCardStatus {
  ACTIVE,
  BLOCKED,
  CLOSED
}