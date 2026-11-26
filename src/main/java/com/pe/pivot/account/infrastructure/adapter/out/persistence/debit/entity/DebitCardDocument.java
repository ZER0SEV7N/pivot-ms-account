package com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.entity;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing a DebitCard stored in db_pivot_account.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "debit_cards")
public class DebitCardDocument {

  @Id
  private String id;
  private String cardNumber;

  @Indexed(unique = true)
  private String cardNumberHash;

  @Indexed
  private String customerId;
  private String customerName;
  private String primaryAccountId;
  private List<String> secondaryAccountIds;
  private String status;
  private Instant createdAt;
  private Instant updatedAt;
}
