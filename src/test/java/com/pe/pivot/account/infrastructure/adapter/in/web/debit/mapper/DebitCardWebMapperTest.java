package com.pe.pivot.account.infrastructure.adapter.in.web.debit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.domain.model.debit.DebitedAccountEntry;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardPaymentResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardUpdateRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitedAccountDetail;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebitCardWebMapper}.
 */
@DisplayName("Should be able to map debit card web payloads")
class DebitCardWebMapperTest {

  private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");

  private DebitCardWebMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new DebitCardWebMapper();
  }

  @Test
  @DisplayName("Should be mapped to a domain when creating a debit card")
  void shouldBeMappedToDomainWhenCreatingDebitCard() {
    // (happy path)
    // Arrange
    DebitCardRequest request = DebitCardRequest.builder()
            .customerId("cust-1")
            .primaryAccountId("acc-1")
            .secondaryAccountIds(List.of("acc-2"))
            .build();

    // Act
    DebitCard card = mapper.toDomain(request);

    // Assert
    assertThat(card.getCustomerId()).isEqualTo("cust-1");
    assertThat(card.getPrimaryAccountId()).isEqualTo("acc-1");
    assertThat(card.getSecondaryAccountIds()).containsExactly("acc-2");
    assertThat(card.getStatus()).isNull();
  }

  @Test
  @DisplayName("Should be mapped to a domain when updating a debit card")
  void shouldBeMappedToDomainWhenUpdatingDebitCard() {
    // Arrange
    DebitCardUpdateRequest request = DebitCardUpdateRequest.builder()
            .primaryAccountId("acc-9")
            .secondaryAccountIds(List.of("acc-2"))
            .status(DebitCardStatus.BLOCKED)
            .build();

    // Act
    DebitCard card = mapper.toDomain(request);

    // Assert
    assertThat(card.getPrimaryAccountId()).isEqualTo("acc-9");
    assertThat(card.getSecondaryAccountIds()).containsExactly("acc-2");
    assertThat(card.getStatus()).isEqualTo(DebitCardStatus.BLOCKED);
    assertThat(card.getCustomerId()).isNull();
  }

  @Test
  @DisplayName("Should be masked when mapping to the standard response")
  void shouldBeMaskedWhenMappingToStandardResponse() {
    // Arrange
    DebitCard card = card();

    // Act
    DebitCardResponse response = mapper.toResponse(card);

    // Assert
    assertThat(response.id()).isEqualTo("dc-1");
    assertThat(response.cardNumber()).isEqualTo("************7890");
    assertThat(response.customerId()).isEqualTo("cust-1");
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
    assertThat(response.primaryAccountId()).isEqualTo("acc-1");
    assertThat(response.secondaryAccountIds()).containsExactly("acc-2");
    assertThat(response.status()).isEqualTo(DebitCardStatus.ACTIVE);
    assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    assertThat(response.updatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  @DisplayName("Should be unmasked when mapping to the creation response")
  void shouldBeUnmaskedWhenMappingToCreationResponse() {
    // Act
    DebitCardResponse response = mapper.toUnmaskedResponse(card());

    // Assert
    assertThat(response.cardNumber()).isEqualTo("4557891234567890");
  }

  @Test
  @DisplayName("Should be mapped with the full breakdown when mapping a payment result")
  void shouldBeMappedWithFullBreakdownWhenMappingPaymentResult() {
    // Arrange
    DebitCardPaymentResult result = DebitCardPaymentResult.builder()
            .debitCardId("dc-1")
            .maskedCardNumber("************7890")
            .totalAmount(new BigDecimal("100.00"))
            .totalCommission(new BigDecimal("1.50"))
            .debitedAccounts(List.of(DebitedAccountEntry.builder()
                    .accountId("acc-1")
                    .maskedAccountNumber("************0-01")
                    .debitedAmount(new BigDecimal("100.00"))
                    .appliedCommission(new BigDecimal("1.50"))
                    .previousBalance(new BigDecimal("500.00"))
                    .newBalance(new BigDecimal("400.00"))
                    .build()))
            .processedAt(CREATED_AT)
            .build();

    // Act
    DebitCardPaymentResponse response = mapper.toPaymentResponse(result);

    // Assert
    assertThat(response.debitCardId()).isEqualTo("dc-1");
    assertThat(response.maskedCardNumber()).isEqualTo("************7890");
    assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
    assertThat(response.totalCommission()).isEqualByComparingTo("1.50");
    assertThat(response.processedAt()).isEqualTo(CREATED_AT);
    assertThat(response.debitedAccounts()).hasSize(1);
    DebitedAccountDetail detail = response.debitedAccounts().get(0);
    assertThat(detail.accountId()).isEqualTo("acc-1");
    assertThat(detail.maskedAccountNumber()).isEqualTo("************0-01");
    assertThat(detail.debitedAmount()).isEqualByComparingTo("100.00");
    assertThat(detail.appliedCommission()).isEqualByComparingTo("1.50");
    assertThat(detail.previousBalance()).isEqualByComparingTo("500.00");
    assertThat(detail.newBalance()).isEqualByComparingTo("400.00");
  }

  @Test
  @DisplayName("Should be mapped empty when the payment has no debited accounts")
  void shouldBeMappedEmptyWhenPaymentHasNoDebitedAccounts() {
    // (error: missing debited accounts)
    // Arrange
    DebitCardPaymentResult result = DebitCardPaymentResult.builder()
            .debitCardId("dc-1")
            .debitedAccounts(List.of())
            .build();

    // Act
    DebitCardPaymentResponse response = mapper.toPaymentResponse(result);

    // Assert
    assertThat(response.debitedAccounts()).isEmpty();
  }

  private DebitCard card() {
    return DebitCard.builder()
            .id("dc-1")
            .cardNumber("4557891234567890")
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
