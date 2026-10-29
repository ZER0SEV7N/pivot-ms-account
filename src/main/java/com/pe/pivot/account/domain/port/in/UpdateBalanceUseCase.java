package com.pe.pivot.account.domain.port.in;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.OperationType;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;

/**
 * Input Port for executing balance updates (deposits and withdrawals) from ms-transaction.
 */
public interface UpdateBalanceUseCase {
    Single<BankAccount> execute(String id, OperationType operationType, BigDecimal amount);
}