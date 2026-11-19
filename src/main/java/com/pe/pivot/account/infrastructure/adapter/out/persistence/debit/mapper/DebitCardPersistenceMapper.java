package com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.mapper;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardStatus;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.entity.DebitCardDocument;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper converting between Domain DebitCard and encrypted MongoDB DebitCardDocument.
 */
@Component
@RequiredArgsConstructor
public class DebitCardPersistenceMapper {

  private final CryptoUtil cryptoUtil;

  /**
   * Maps a DebitCard domain object to a DebitCardDocument for persistence in MongoDB.
   *
   * @param domain The DebitCard domain object to be converted.
   * @return The corresponding DebitCardDocument for MongoDB storage.
   */
  public DebitCardDocument toDocument(DebitCard domain) {
    return DebitCardDocument.builder()
            .id(domain.getId())
            .cardNumber(cryptoUtil.encryptAes256(domain.getCardNumber()))
            .cardNumberHash(domain.getCardNumberHash())
            .customerId(domain.getCustomerId())
            .customerName(domain.getCustomerName())
            .primaryAccountId(domain.getPrimaryAccountId())
            .secondaryAccountIds(domain.getSecondaryAccountIds())
            .status(domain.getStatus() != null ? domain.getStatus().name() : null)
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
  }

  /**
   * Maps a DebitCardDocument from MongoDB to a DebitCard domain object.
   *
   * @param doc The DebitCardDocument retrieved from MongoDB.
   * @return The corresponding DebitCard domain object.
   */
  public DebitCard toDomain(DebitCardDocument doc) {
    return DebitCard.builder()
            .id(doc.getId())
            .cardNumber(cryptoUtil.decryptAes256(doc.getCardNumber()))
            .cardNumberHash(doc.getCardNumberHash())
            .customerId(doc.getCustomerId())
            .customerName(doc.getCustomerName())
            .primaryAccountId(doc.getPrimaryAccountId())
            .secondaryAccountIds(doc.getSecondaryAccountIds())
            .status(doc.getStatus() != null ? DebitCardStatus.valueOf(doc.getStatus()) : null)
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .build();
  }
}
