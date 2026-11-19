package com.pe.pivot.account.infrastructure.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.mapper.AccountWebMapper;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountWebMapper.
 */
class AccountWebMapperTest {

  private AccountWebMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AccountWebMapper();
  }

  @Test
  @DisplayName("Should map an AccountRequest to the domain model")
  void shouldMapRequestToDomain() {
    // Arrange
    AccountRequest request = request();

    // Act
    BankAccount account = mapper.toDomain(request);

    // Assert
    assertThat(account.getAccountNumber()).isEqualTo("191-00000000-0-01");
    assertThat(account.getCustomerId()).isEqualTo("customer-1");
    assertThat(account.getAccountType()).isEqualTo(AccountType.SAVINGS);
    assertThat(account.getCurrency()).isEqualTo("PEN");
    assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    assertThat(account.getMaintenanceFee()).isEqualByComparingTo("0");
    assertThat(account.getMinimumDailyAverage()).isEqualByComparingTo("0");
    assertThat(account.getMinOpeningAmount()).isEqualByComparingTo("0");
    assertThat(account.getMaxMonthlyMovements()).isEqualTo(5);
    assertThat(account.getMaxFreeMonthlyTransactions()).isEqualTo(3);
    assertThat(account.getTransactionCommissionFee()).isEqualByComparingTo("5.00");
    assertThat(account.getAllowedMovementDay()).isEqualTo(28);
    assertThat(account.getHolders()).containsExactly("holder-1");
    assertThat(account.getAuthorizedSigners()).containsExactly("signer-1");
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should expose the full account number in the creation response")
  void shouldBuildUnmaskedResponse() {
    // Arrange
    BankAccount account = domain();

    // Act
    AccountResponse response = mapper.toUnmaskedResponse(account);

    // Assert
    assertThat(response.accountNumber()).isEqualTo("191-00000000-0-01");
    assertThat(response.id()).isEqualTo("account-1");
    assertThat(response.customerId()).isEqualTo("customer-1");
    assertThat(response.accountType()).isEqualTo(AccountType.SAVINGS);
    assertThat(response.balance()).isEqualByComparingTo("100.00");
    assertThat(response.holders()).containsExactly("holder-1");
    assertThat(response.authorizedSigners()).containsExactly("signer-1");
    assertThat(response.createdAt()).isNotNull();
    assertThat(response.updatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should mask the account number in regular responses")
  void shouldBuildMaskedResponse() {
    // Arrange
    BankAccount account = domain();

    // Act
    AccountResponse response = mapper.toResponse(account);

    // Assert
    assertThat(response.accountNumber()).isEqualTo("*************0-01");
  }

  @Test
  @DisplayName("Should map a domain account to a balance response")
  void shouldMapToBalanceResponse() {
    // Arrange
    BankAccount account = domain();

    // Act
    BalanceResponse response = mapper.toBalanceResponse(account);

    // Assert
    assertThat(response.accountId()).isEqualTo("account-1");
    assertThat(response.accountNumber()).isEqualTo("*************0-01");
    assertThat(response.accountType()).isEqualTo(AccountType.SAVINGS);
    assertThat(response.currency()).isEqualTo("PEN");
    assertThat(response.availableBalance()).isEqualByComparingTo("100.00");
    assertThat(response.currentMonthlyMovements()).isEqualTo(2);
    assertThat(response.maxMonthlyMovements()).isEqualTo(5);
    assertThat(response.queriedAt()).isNotNull();
  }

  private AccountRequest request() {
    return AccountRequest.builder()
        .accountNumber("191-00000000-0-01")
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
        .allowedMovementDay(28)
        .holders(List.of("holder-1"))
        .authorizedSigners(List.of("signer-1"))
        .status(AccountStatus.ACTIVE)
        .build();
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
}
