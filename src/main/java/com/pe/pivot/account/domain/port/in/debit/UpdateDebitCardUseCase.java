package com.pe.pivot.account.domain.port.in.debit;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for updating an existing debit card.
 */
public interface UpdateDebitCardUseCase {

  /**
   * Updates an existing debit card.
   *
   * @param debitCard the debit card to update
   * @return Single emitting the updated DebitCard, or an error if the update fails
   */
  Single<DebitCard> update(DebitCard debitCard);
}
