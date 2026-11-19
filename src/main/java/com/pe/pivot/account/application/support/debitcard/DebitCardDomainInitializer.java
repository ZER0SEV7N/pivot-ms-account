package com.pe.pivot.account.application.support.debitcard;

import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Initializes default fields, card number, SHA-256 blind index, and sanitized account links
 * for new DebitCard entities (SRP).
 */
@Component
@RequiredArgsConstructor
public class DebitCardDomainInitializer {

  private static final String CARD_BIN_PREFIX = "455789"; // Example BIN prefix for Visa cards
  private static final int RANDOM_DIGITS = 10; // Number of random digits to generate for the card number after the BIN prefix
  private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // Thread-safe random number generator for card number generation

  private final CryptoUtil crypto;

  /**
   * Populates generated card number, blind-index hash, customer display name, and timestamps.
   *
   * @param customer the verified customer summary
   * @param debitCard the debit card to initialize
   */
  public void initialize(CustomerSummary customer, DebitCard debitCard) {
    // Generate a secure random debit card number with the specified BIN prefix and random digits
    String generatedNumber = generateCardNumber();
    debitCard.setCardNumber(generatedNumber);
    debitCard.setCardNumberHash(crypto.hashSha256(generatedNumber));
    debitCard.setCustomerName(customer.getDisplayName());
    debitCard.setSecondaryAccountIds(
            sanitizeSecondaryIds(debitCard.getPrimaryAccountId(), debitCard.getSecondaryAccountIds())
    );
    debitCard.setStatus(DebitCardStatus.ACTIVE);
    Instant now = Instant.now();
    debitCard.setCreatedAt(now);
    debitCard.setUpdatedAt(now);
  }

  /**
   * Sanitizes the list of secondary account IDs by removing null, blank, and duplicate entries,
   *
   * @param primaryAccountId the primary account ID to exclude from the secondary list
   * @param secondaryIds the list of secondary account IDs to sanitize
   * @return a sanitized list of unique secondary account IDs
   */
  public List<String> sanitizeSecondaryIds(String primaryAccountId, List<String> secondaryIds) {
    // Remove null, blank, and duplicate secondary account IDs, ensuring the primary account ID is excluded
    if (secondaryIds == null || secondaryIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Filter out invalid secondary IDs and ensure uniqueness while preserving order
    return secondaryIds.stream()
            .filter(id -> id != null && !id.isBlank() && !id.equals(primaryAccountId))
            .distinct()
            .toList();
  }

  /**
   * Generates a secure random debit card number with the specified BIN prefix and random digits.
   *
   * @return a generated debit card number as a String
   */
  private String generateCardNumber() {
    StringBuilder sb = new StringBuilder(CARD_BIN_PREFIX);
    for (int i = 0; i < RANDOM_DIGITS; i++) {
      sb.append(SECURE_RANDOM.nextInt(10));
    }
    return sb.toString();
  }
}
