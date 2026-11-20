package com.pe.pivot.account.application.support.debitcard;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebitCardDomainInitializer}.
 */
@DisplayName("Should be able to initialize a new debit card")
class DebitCardDomainInitializerTest {

  private CryptoUtil cryptoUtil;
  private DebitCardDomainInitializer initializer;

  @BeforeEach
  void setUp() {
    cryptoUtil = new CryptoUtil("unit-test-secret-key");
    initializer = new DebitCardDomainInitializer(cryptoUtil);
  }

  @Test
  @DisplayName("Should be initialized with a generated card number and sanitized links")
  void shouldBeInitializedWithGeneratedCardNumberAndSanitizedLinks() {
    // (happy path)
    // Arrange
    CustomerSummary customer = CustomerSummary.builder()
            .customerType("PERSONAL")
            .firstName("Ada")
            .lastName("Lovelace")
            .build();
    DebitCard card = DebitCard.builder()
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2", " ", "acc-2", "acc-1", "acc-3"))
            .build();

    // Act
    initializer.initialize(customer, card);

    // Assert
    assertThat(card.getCardNumber()).startsWith("455789").hasSize(16);
    assertThat(card.getCardNumberHash())
            .isEqualTo(cryptoUtil.hashSha256(card.getCardNumber()));
    assertThat(card.getCustomerName()).isEqualTo("Ada Lovelace");
    assertThat(card.getSecondaryAccountIds()).containsExactly("acc-2", "acc-3");
    assertThat(card.getStatus()).isEqualTo(DebitCardStatus.ACTIVE);
    assertThat(card.getCreatedAt()).isNotNull();
    assertThat(card.getUpdatedAt()).isEqualTo(card.getCreatedAt());
  }

  @Test
  @DisplayName("Should be initialized with the business name when available")
  void shouldBeInitializedWithBusinessNameWhenAvailable() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder()
            .customerType("BUSINESS")
            .businessName("  Pivot SAC  ")
            .firstName("Ignored")
            .build();
    DebitCard card = DebitCard.builder().primaryAccountId("acc-1").build();

    // Act
    initializer.initialize(customer, card);

    // Assert
    assertThat(card.getCustomerName()).isEqualTo("Pivot SAC");
    assertThat(card.getSecondaryAccountIds()).isEmpty();
  }

  @Test
  @DisplayName("Should be sanitized to empty when the secondary list is null")
  void shouldBeSanitizedToEmptyWhenSecondaryListIsNull() {
    // (error: missing secondary accounts)
    // Act
    List<String> sanitized = initializer.sanitizeSecondaryIds("acc-1", null);

    // Assert
    assertThat(sanitized).isEmpty();
  }

  @Test
  @DisplayName("Should be sanitized to empty when the secondary list is empty")
  void shouldBeSanitizedToEmptyWhenSecondaryListIsEmpty() {
    // Act
    List<String> sanitized = initializer.sanitizeSecondaryIds("acc-1", List.of());

    // Assert
    assertThat(sanitized).isEmpty();
  }

  @Test
  @DisplayName("Should be sanitized removing nulls, blanks, duplicates and the primary account")
  void shouldBeSanitizedRemovingInvalidAndDuplicateIds() {
    // Arrange
    List<String> secondary = java.util.Arrays.asList("acc-2", null, "  ", "acc-2", "acc-1");

    // Act
    List<String> sanitized = initializer.sanitizeSecondaryIds("acc-1", secondary);

    // Assert
    assertThat(sanitized).containsExactly("acc-2");
  }
}
