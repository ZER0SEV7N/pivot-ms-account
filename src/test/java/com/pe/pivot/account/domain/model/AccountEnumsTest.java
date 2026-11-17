package com.pe.pivot.account.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountType and AccountStatus enumerations.
 */
class AccountEnumsTest {

  @Test
  @DisplayName("Should resolve all AccountType and AccountStatus enum constants")
  void shouldResolveEnumConstants() {
    // Assert
    assertEquals(3, AccountType.values().length);
    assertEquals(3, AccountStatus.values().length);
    assertNotNull(AccountType.valueOf("SAVINGS"));
    assertNotNull(AccountType.valueOf("CHECKING"));
    assertNotNull(AccountType.valueOf("FIXED_TERM"));
    assertNotNull(AccountStatus.valueOf("ACTIVE"));
    assertNotNull(AccountStatus.valueOf("BLOCKED"));
    assertNotNull(AccountStatus.valueOf("CLOSED"));
  }
}
