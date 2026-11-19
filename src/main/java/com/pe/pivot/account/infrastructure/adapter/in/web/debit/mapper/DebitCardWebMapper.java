package com.pe.pivot.account.infrastructure.adapter.in.web.debit.mapper;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.model.debit.DebitedAccountEntry;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardPaymentResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardUpdateRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitedAccountDetail;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Web Mapper translating between REST DTOs and DebitCard domain models.
 */
@Component
public class DebitCardWebMapper {

  /**
   * Maps a DebitCardRequest DTO to a DebitCard domain object.
   *
   * @param request The DebitCardRequest DTO containing the request data.
   * @return The corresponding DebitCard domain object.
   */
  public DebitCard toDomain(DebitCardRequest request) {
    return DebitCard.builder()
            .customerId(request.customerId())
            .primaryAccountId(request.primaryAccountId())
            .secondaryAccountIds(request.secondaryAccountIds())
            .build();
  }

  /**
   * Maps a DebitCardUpdateRequest DTO to a DebitCard domain object.
   *
   * @param request The DebitCardUpdateRequest DTO containing the update data.
   * @return The corresponding DebitCard domain object with updated fields.
   */
  public DebitCard toDomain(DebitCardUpdateRequest request) {
    return DebitCard.builder()
            .primaryAccountId(request.primaryAccountId())
            .secondaryAccountIds(request.secondaryAccountIds())
            .status(request.status())
            .build();
  }

  /**
   * Maps a DebitCard domain object to a DebitCardResponse DTO, revealing the full card number.
   *
   * @param card The DebitCard domain object to be converted.
   * @return The corresponding DebitCardResponse DTO with the full card number.
   */
  public DebitCardResponse toUnmaskedResponse(DebitCard card) {
    return buildResponse(card, card.getCardNumber());
  }

  /**
   * Maps a DebitCard domain object to a DebitCardResponse DTO, masking the card number for security.
   *
   * @param card The DebitCard domain object to be converted.
   * @return The corresponding DebitCardResponse DTO with the masked card number.
   */
  public DebitCardResponse toResponse(DebitCard card) {
    return buildResponse(card, DataMaskingUtil.maskAccountNumber(card.getCardNumber()));
  }

  /**
   * Builds a DebitCardResponse DTO from a DebitCard domain object and a specified card number.
   *
   * @param result The DebitCard domain object containing the card details.
   * @return The corresponding DebitCardResponse DTO with the specified card number.
   */
  public DebitCardPaymentResponse toPaymentResponse(DebitCardPaymentResult result) {
    List<DebitedAccountDetail> details = result.debitedAccounts().stream()
            .map(this::toDetail)
            .toList();

    return DebitCardPaymentResponse.builder()
            .debitCardId(result.debitCardId())
            .maskedCardNumber(result.maskedCardNumber())
            .totalAmount(result.totalAmount())
            .totalCommission(result.totalCommission())
            .debitedAccounts(details)
            .processedAt(result.processedAt())
            .build();
  }

  /**
   * Builds a DebitCardResponse DTO from a DebitCard domain object and a specified display card number.
   *
   * @param card The DebitCard domain object containing the card details.
   * @param displayCardNumber The card number to be displayed in the response (masked or unmasked).
   * @return The corresponding DebitCardResponse DTO with the specified display card number.
   */
  private DebitCardResponse buildResponse(DebitCard card, String displayCardNumber) {
    return DebitCardResponse.builder()
            .id(card.getId())
            .cardNumber(displayCardNumber)
            .customerId(card.getCustomerId())
            .customerName(card.getCustomerName())
            .primaryAccountId(card.getPrimaryAccountId())
            .secondaryAccountIds(card.getSecondaryAccountIds())
            .status(card.getStatus())
            .createdAt(card.getCreatedAt())
            .updatedAt(card.getUpdatedAt())
            .build();
  }

  /**
   * Maps a DebitedAccountEntry domain object to a DebitedAccountDetail DTO.
   *
   * @param entry The DebitedAccountEntry domain object containing the account debit details.
   * @return The corresponding DebitedAccountDetail DTO with the account debit information.
   */
  private DebitedAccountDetail toDetail(DebitedAccountEntry entry) {
    return DebitedAccountDetail.builder()
            .accountId(entry.accountId())
            .maskedAccountNumber(entry.maskedAccountNumber())
            .debitedAmount(entry.debitedAmount())
            .appliedCommission(entry.appliedCommission())
            .previousBalance(entry.previousBalance())
            .newBalance(entry.newBalance())
            .build();
  }
}