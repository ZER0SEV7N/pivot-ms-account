package com.pe.pivot.account.application.support.debitcard;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import java.util.List;

/**
 * Immutable holder pairing the mutated BankAccounts to persist with the payment result summary.
 */
public record CascadingExecution(
        List<BankAccount> modifiedAccounts,
        DebitCardPaymentResult result
) {
}