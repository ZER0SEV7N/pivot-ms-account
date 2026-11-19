package com.pe.pivot.account.application.usecase.debitcard;

import com.pe.pivot.account.application.support.debitcard.CascadingDebitPolicy;
import com.pe.pivot.account.application.support.debitcard.CascadingExecution;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.DebitCardNotFoundException;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.in.debit.PayWithDebitCardUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * Pure Orchestrator Use Case for debit card payments, delegating cascading allocation
 * rules to CascadingDebitPolicy (SRP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayWithDebitCardUseCaseImpl implements PayWithDebitCardUseCase {

  private final DebitCardRepositoryPort debitCardRepository;
  private final FindAccountByIdUseCase findAccountByIdUseCase;
  private final AccountRepositoryPort accountRepository;
  private final CascadingDebitPolicy cascadingDebitPolicy;

  /**
   * Processes a payment using a debit card, applying cascading allocation rules to linked accounts.
   *
   * @param debitCardId the unique identifier of the debit card
   * @param amount      the amount to be paid
   * @return Single emitting the result of the debit card payment, or an error if the payment fails
   */
  @Override
  @NonNull
  public Single<DebitCardPaymentResult> pay (String debitCardId, BigDecimal amount) {
    // Validate that the payment amount is greater than zero
    if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return Single.error(
              new BusinessRuleException("Payment amount must be greater than zero."));
    }

    return debitCardRepository.findById(debitCardId)
            .switchIfEmpty(Single.error(
                    new DebitCardNotFoundException("Debit Card not found with ID: " + debitCardId)
            ))
            // Validate that the debit card is ACTIVE before proceeding with the payment
            .flatMap(this::validateCardActive)
            .flatMap(card -> Flowable.fromIterable(card.getOrderedAccountIds())
                    .concatMapSingle(findAccountByIdUseCase::findById)
                    .toList()
                    // After retrieving all linked accounts,
                    // execute the cascading debit policy to allocate the payment amount across them
                    .flatMap(accounts -> {
                      CascadingExecution execution = cascadingDebitPolicy.execute(card, accounts, amount);
                      return Flowable.fromIterable(execution.modifiedAccounts())
                              .concatMapSingle(accountRepository::save)
                              .ignoreElements()
                              .andThen(Single.just(execution.result()));
                    }))
            // Log the result of the debit card payment for auditing and monitoring purposes
            .doOnSuccess(res -> log.info(
                    "Debit card payment completed. CardId: {}, Total: {}, AccountsDebited: {}",
                    res.debitCardId(), res.totalAmount(), res.debitedAccounts().size()
            ));
  }

  /**
   * Validates that the debit card is ACTIVE before allowing a payment to proceed.
   *
   * @param card the debit card to validate
   * @return Single emitting the debit card if it is ACTIVE, or an error if it is not
   */
  private Single<DebitCard> validateCardActive(@NonNull DebitCard card) {
    if (!card.isActive()) {
      return Single.error(new BusinessRuleException(
              "Debit card is not ACTIVE. Current status: " + card.getStatus()));
    }
    return Single.just(card);
  }
}
