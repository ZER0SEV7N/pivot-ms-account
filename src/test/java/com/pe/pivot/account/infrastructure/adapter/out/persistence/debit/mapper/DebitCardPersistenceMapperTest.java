package com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.entity.DebitCardDocument;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebitCardPersistenceMapper}.
 */
@DisplayName("Should be able to map debit cards to and from MongoDB documents")
class DebitCardPersistenceMapperTest {

  private static final String CARD_NUMBER = "4557891234567890";
  private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");

  private CryptoUtil cryptoUtil;
  private DebitCardPersistenceMapper mapper;

  @BeforeEach
  void setUp() {
    cryptoUtil = new CryptoUtil("unit-test-secret-key");
    mapper = new DebitCardPersistenceMapper(cryptoUtil);
  }

  @Test
  @DisplayName("Should be encrypted when mapping the domain to a document")
  void shouldBeEncryptedWhenMappingDomainToDocument() {
    // (happy path)
    // Act
    DebitCardDocument document = mapper.toDocument(domain());

    // Assert
    assertThat(document.getId()).isEqualTo("dc-1");
    assertThat(document.getCardNumber()).isNotEqualTo(CARD_NUMBER);
    assertThat(cryptoUtil.decryptAes256(document.getCardNumber())).isEqualTo(CARD_NUMBER);
    assertThat(document.getCardNumberHash()).isEqualTo("hash-value");
    assertThat(document.getCustomerId()).isEqualTo("cust-1");
    assertThat(document.getCustomerName()).isEqualTo("Ada Lovelace");
    assertThat(document.getPrimaryAccountId()).isEqualTo("acc-1");
    assertThat(document.getSecondaryAccountIds()).containsExactly("acc-2");
    assertThat(document.getStatus()).isEqualTo("ACTIVE");
    assertThat(document.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(document.getUpdatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  @DisplayName("Should be mapped with a null status when the domain has no status")
  void shouldBeMappedWithNullStatusWhenDomainHasNoStatus() {
    // (error: missing status)
    // Arrange
    DebitCard domain = domain();
    domain.setStatus(null);

    // Act
    DebitCardDocument document = mapper.toDocument(domain);

    // Assert
    assertThat(document.getStatus()).isNull();
  }

  @Test
  @DisplayName("Should be decrypted when mapping the document to the domain")
  void shouldBeDecryptedWhenMappingDocumentToDomain() {
    // Arrange
    DebitCardDocument document = DebitCardDocument.builder()
            .id("dc-1")
            .cardNumber(cryptoUtil.encryptAes256(CARD_NUMBER))
            .cardNumberHash("hash-value")
            .customerId("cust-1")
            .customerName("Ada Lovelace")
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .status("ACTIVE")
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();

    // Act
    DebitCard domain = mapper.toDomain(document);

    // Assert
    assertThat(domain.getId()).isEqualTo("dc-1");
    assertThat(domain.getCardNumber()).isEqualTo(CARD_NUMBER);
    assertThat(domain.getCardNumberHash()).isEqualTo("hash-value");
    assertThat(domain.getCustomerId()).isEqualTo("cust-1");
    assertThat(domain.getCustomerName()).isEqualTo("Ada Lovelace");
    assertThat(domain.getPrimaryAccountId()).isEqualTo("acc-1");
    assertThat(domain.getSecondaryAccountIds()).containsExactly("acc-2");
    assertThat(domain.getStatus()).isEqualTo(DebitCardStatus.ACTIVE);
    assertThat(domain.getCreatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  @DisplayName("Should be mapped with a null status when the document has no status")
  void shouldBeMappedWithNullStatusWhenDocumentHasNoStatus() {
    // (error: missing persisted status)
    // Arrange
    DebitCardDocument document = DebitCardDocument.builder()
            .id("dc-1")
            .cardNumber(cryptoUtil.encryptAes256(CARD_NUMBER))
            .build();

    // Act
    DebitCard domain = mapper.toDomain(document);

    // Assert
    assertThat(domain.getStatus()).isNull();
  }

  private DebitCard domain() {
    return DebitCard.builder()
            .id("dc-1")
            .cardNumber(CARD_NUMBER)
            .cardNumberHash("hash-value")
            .customerId("cust-1")
            .customerName("Ada Lovelace")
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .status(DebitCardStatus.ACTIVE)
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();
  }
}
