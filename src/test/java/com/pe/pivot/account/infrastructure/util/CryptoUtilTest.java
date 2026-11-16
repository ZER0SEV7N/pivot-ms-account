package com.pe.pivot.account.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CryptoUtil.
 *
 * <p>Validates SHA-256 blind indexing and AES-256-GCM encryption round trips.
 */
class CryptoUtilTest {

  private static final String SECRET_KEY = "unit-test-secret-key";

  private CryptoUtil cryptoUtil;

  @BeforeEach
  void setUp() {
    cryptoUtil = new CryptoUtil(SECRET_KEY);
  }

  @Test
  @DisplayName("Should hash a value using SHA-256")
  void shouldHashValueUsingSha256() {
    // Act
    String hash = cryptoUtil.hashSha256("12345678");

    // Assert
    assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
  }

  @Test
  @DisplayName("Should trim the input before hashing")
  void shouldTrimInputBeforeHashing() {
    // Act
    String hash = cryptoUtil.hashSha256("  12345678  ");

    // Assert
    assertThat(hash).isEqualTo(cryptoUtil.hashSha256("12345678"));
  }

  @Test
  @DisplayName("Should return null when hashing a null value")
  void shouldReturnNullWhenHashingNull() {
    // Act / Assert
    assertThat(cryptoUtil.hashSha256(null)).isNull();
  }

  @Test
  @DisplayName("Should encrypt and decrypt a value using AES-256")
  void shouldEncryptAndDecryptValue() {
    // Arrange
    String plainText = "191-12345678-0-01";

    // Act
    String encrypted = cryptoUtil.encryptAes256(plainText);
    String decrypted = cryptoUtil.decryptAes256(encrypted);

    // Assert
    assertThat(encrypted).isNotEqualTo(plainText);
    assertThat(decrypted).isEqualTo(plainText);
  }

  @Test
  @DisplayName("Should return null when encrypting a null value")
  void shouldReturnNullWhenEncryptingNull() {
    // Act / Assert
    assertThat(cryptoUtil.encryptAes256(null)).isNull();
  }

  @Test
  @DisplayName("Should return null when decrypting a null value")
  void shouldReturnNullWhenDecryptingNull() {
    // Act / Assert
    assertThat(cryptoUtil.decryptAes256(null)).isNull();
  }

  @Test
  @DisplayName("Should reject an encrypted payload that is too short")
  void shouldRejectTooShortEncryptedPayload() {
    // Arrange
    String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[8]);

    // Act / Assert
    assertThatThrownBy(() -> cryptoUtil.decryptAes256(tooShort))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Error decrypting data with AES-256");
  }

  @Test
  @DisplayName("Should reject a payload that is not valid Base64")
  void shouldRejectInvalidBase64Payload() {
    // Act / Assert
    assertThatThrownBy(() -> cryptoUtil.decryptAes256("not-a-valid-base64!!!"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Error decrypting data with AES-256");
  }

  @Test
  @DisplayName("Should reject a blank AES secret key")
  void shouldRejectBlankSecretKey() {
    // Act / Assert
    assertThatThrownBy(() -> new CryptoUtil("   "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AES secret key is missing");
  }

  @Test
  @DisplayName("Should reject a null AES secret key")
  void shouldRejectNullSecretKey() {
    // Act / Assert
    assertThatThrownBy(() -> new CryptoUtil(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AES secret key is missing");
  }
}
