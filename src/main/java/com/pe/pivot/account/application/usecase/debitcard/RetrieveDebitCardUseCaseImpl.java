package com.pe.pivot.account.application.usecase.debitcard;

import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.in.debit.RetrieveDebitCardUseCase;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use Case for retrieving DebitCard records and querying the primary account balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveDebitCardUseCaseImpl implements RetrieveDebitCardUseCase {

  private final DebitCardRepositoryPort debitCardRepository;
  private final FindAccountByIdUseCase findAccountByIdUseCase;
  private final CryptoUtil cryptoUtil;

  /**
   * Retrieves all debit cards in the system.
   *
   * @return Flowable emitting all DebitCards
   */
  @Override
  public Flowable<DebitCard> findAll() {
    log.debug("Retrieving all debit cards");
    return debitCardRepository.findAll();
  }

  /**
   * Retrieves a debit card by its unique identifier.
   *
   * @param id the unique identifier of the debit card
   * @return Single emitting the DebitCard if found, or an error if not found
   */
  @Override
  public Single<DebitCard> findById(String id) {
    log.debug("Retrieving debit card by ID: {}", id);
    return debitCardRepository.findById(id)
            .switchIfEmpty(Single.error(
                    new DebitCardNotFoundException("Debit card not found with ID: " + id)));
  }

  /**
   * Retrieves a debit card by its card number using a SHA-256 blind index hash for security.
   *
   * @param cardNumber the unique card number of the debit card
   * @return Single emitting the DebitCard if found, or an error if not found
   */
  @Override
  public Single<DebitCard> findByCardNumber(String cardNumber) {
    log.debug("Retrieving debit card via SHA-256 blind index");
    String hash = cryptoUtil.hashSha256(cardNumber);
    return debitCardRepository.findByCardNumberHash(hash)
            .switchIfEmpty(Single.error(
                    new DebitCardNotFoundException("Debit card not found for the provided number."))
            );
  }

  /**
   * Retrieves all debit cards associated with a given customer ID.
   *
   * @param customerId the unique identifier of the customer
   * @return Flowable emitting all DebitCards associated with the customer
   */
  @Override
  public Flowable<DebitCard> findByCustomerId(String customerId) {
    log.debug("Retrieving debit cards for customerId: {}", customerId);
    return debitCardRepository.findByCustomerId(customerId);
  }

  /**
   * Retrieves the primary account balance associated with a given debit card ID.
   *
   * @param debitCardId the unique identifier of the debit card
   * @return Single emitting the primary account balance as a BankAccount, or an error if not found
   */
  @Override
  public Single<BankAccount> getPrimaryAccountBalance(String debitCardId) {
    log.debug("Consulting primary account balance for debitCardId: {}", debitCardId);
    return findById(debitCardId)
            .flatMap(card -> findAccountByIdUseCase.findById(card.getPrimaryAccountId()));
  }
}