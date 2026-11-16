package com.pe.pivot.account.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.entity.BankAccountDocument;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountPersistenceMapper.
 */
class AccountPersistenceMapperTest {

  private CryptoUtil cryptoUtil;
  private AccountPersistenceMapper mapper;

  @BeforeEach
  void setUp() {
    cryptoUtil = new CryptoUtil("unit-test-secret-key");
    mapper = new AccountPersistenceMapper(cryptoUtil);
  }

  @Test
  @DisplayName("Should map a domain account to an encrypted MongoDB document")
  void shouldMapDomainToDocument() {
    // Arrange
    BankAccount account = domain();

    // Act
    BankAccountDocument document = mapper.toDocument(account);

    // Assert
    assertThat(document.getAccountNumber()).isNotEqualTo("191-00000000-0-01");
    assertThat(cryptoUtil.decryptAes256(document.getAccountNumber()))
        .isEqualTo("191-00000000-0-01");
    assertThat(document.getAccountNumberHash()).isEqualTo("hash");
    assertThat(document.getCustomerId()).isEqualTo("customer-1");
    assertThat(document.getAccountType()).isEqualTo("SAVINGS");
    assertThat(document.getStatus()).isEqualTo("ACTIVE");
    assertThat(document.getCurrency()).isEqualTo("PEN");
    assertThat(document.getBalance()).isEqualByComparingTo("100.00");
    assertThat(document.getHolders()).containsExactly("holder-1");
    assertThat(document.getAuthorizedSigners()).containsExactly("signer-1");
    assertThat(document.getCreatedAt()).isNotNull();
    assertThat(document.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should map a MongoDB document back to the domain model")
  void shouldMapDocumentToDomain() {
    // Arrange
    BankAccountDocument document = document();

    // Act
    BankAccount account = mapper.toDomain(document);

    // Assert
    assertThat(account.getId()).isEqualTo("account-1");
    assertThat(account.getAccountNumber()).isEqualTo("191-00000000-0-01");
    assertThat(account.getAccountNumberHash()).isEqualTo("hash");
    assertThat(account.getCustomerId()).isEqualTo("customer-1");
    assertThat(account.getAccountType()).isEqualTo(AccountType.SAVINGS);
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(account.getCurrency()).isEqualTo("PEN");
    assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    assertThat(account.getHolders()).containsExactly("holder-1");
    assertThat(account.getAuthorizedSigners()).containsExactly("signer-1");
    assertThat(account.getCreatedAt()).isNotNull();
    assertThat(account.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should handle null account type and status during document mapping")
  void shouldHandleNullTypeAndStatusWhenMappingToDocument() {
    // Arrange
    BankAccount account = BankAccount.builder().build();

    // Act
    BankAccountDocument document = mapper.toDocument(account);

    // Assert
    assertThat(document.getAccountType()).isNull();
    assertThat(document.getStatus()).isNull();
    assertThat(document.getAccountNumber()).isNull();
  }

  @Test
  @DisplayName("Should handle null account type and status during domain mapping")
  void shouldHandleNullTypeAndStatusWhenMappingToDomain() {
    // Arrange
    BankAccountDocument document = BankAccountDocument.builder().build();

    // Act
    BankAccount account = mapper.toDomain(document);

    // Assert
    assertThat(account.getAccountType()).isNull();
    assertThat(account.getStatus()).isNull();
    assertThat(account.getAccountNumber()).isNull();
  }

  private BankAccount domain() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    return BankAccount.builder()
        .id("account-1")
        .accountNumber("191-00000000-0-01")
        .accountNumberHash("hash")
        .customerId("customer-1")
        .accountType(AccountType.SAVINGS)
        .currency("PEN")
        .balance(new BigDecimal("100.00"))
        .maintenanceFee(BigDecimal.ZERO)
        .minimumDailyAverage(BigDecimal.ZERO)
        .minOpeningAmount(BigDecimal.ZERO)
        .maxMonthlyMovements(5)
        .maxFreeMonthlyTransactions(3)
        .transactionCommissionFee(new BigDecimal("5.00"))
        .lastAppliedCommission(BigDecimal.ZERO)
        .currentMonthlyMovements(2)
        .allowedMovementDay(28)
        .holders(List.of("holder-1"))
        .authorizedSigners(List.of("signer-1"))
        .status(AccountStatus.ACTIVE)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private BankAccountDocument document() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    return BankAccountDocument.builder()
        .id("account-1")
        .accountNumber(cryptoUtil.encryptAes256("191-00000000-0-01"))
        .accountNumberHash("hash")
        .customerId("customer-1")
        .accountType("SAVINGS")
        .currency("PEN")
        .balance(new BigDecimal("100.00"))
        .maintenanceFee(BigDecimal.ZERO)
        .minimumDailyAverage(BigDecimal.ZERO)
        .minOpeningAmount(BigDecimal.ZERO)
        .maxMonthlyMovements(5)
        .maxFreeMonthlyTransactions(3)
        .transactionCommissionFee(new BigDecimal("5.00"))
        .lastAppliedCommission(BigDecimal.ZERO)
        .currentMonthlyMovements(2)
        .allowedMovementDay(28)
        .holders(List.of("holder-1"))
        .authorizedSigners(List.of("signer-1"))
        .status("ACTIVE")
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
