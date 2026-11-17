package com.pe.pivot.account;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for MsAccountApplication class definition.
 */
class MsAccountApplicationTests {

  @Test
  @DisplayName("Should instantiate MsAccountApplication successfully")
  void shouldInstantiateApplicationSuccessfully() {
    // Arrange & Act
    MsAccountApplication application = new MsAccountApplication();

    // Assert
    assertNotNull(application);
  }
}
