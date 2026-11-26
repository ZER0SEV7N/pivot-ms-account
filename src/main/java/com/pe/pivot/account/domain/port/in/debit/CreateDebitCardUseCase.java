package com.pe.pivot.account.domain.port.in.debit;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for creating a new debit card.
 */
public interface CreateDebitCardUseCase {

  /**
   * Creates a new DebitCard in the system.
   *
   * @param debitCard the DebitCard domain model to create
   * @return Single emitting the created DebitCard
   */
  Single<DebitCard> create(DebitCard debitCard);
}
