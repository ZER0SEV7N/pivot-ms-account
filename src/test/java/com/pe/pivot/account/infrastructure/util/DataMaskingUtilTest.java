package com.pe.pivot.account.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DataMaskingUtil.
 */
class DataMaskingUtilTest {

  @Test
  @DisplayName("Should return the fixed mask when the account number is null")
  void shouldMaskNullAccountNumber() {
    // Act / Assert
    assertThat(DataMaskingUtil.maskAccountNumber(null)).isEqualTo("****");
  }

  @Test
  @DisplayName("Should return the fixed mask when the account number is short")
  void shouldMaskShortAccountNumber() {
    // Act / Assert
    assertThat(DataMaskingUtil.maskAccountNumber("123")).isEqualTo("****");
  }

  @Test
  @DisplayName("Should keep the last four characters and mask the rest")
  void shouldKeepLastFourCharacters() {
    // Act / Assert
    assertThat(DataMaskingUtil.maskAccountNumber("12345678")).isEqualTo("****5678");
  }

  @Test
  @DisplayName("Should mask only the leading characters for long account numbers")
  void shouldMaskLongAccountNumber() {
    // Act / Assert
    assertThat(DataMaskingUtil.maskAccountNumber("191-00000000-0-01"))
        .isEqualTo("*************0-01");
  }

  @Test
  @DisplayName("Should not mask an account number with exactly four characters")
  void shouldNotMaskFourCharacterAccountNumber() {
    // Act / Assert
    assertThat(DataMaskingUtil.maskAccountNumber("1234")).isEqualTo("1234");
  }
}
