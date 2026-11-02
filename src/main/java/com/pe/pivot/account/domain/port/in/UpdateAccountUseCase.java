package com.pe.pivot.account.domain.port.in;

import com.pe.pivot.account.domain.model.BankAccount;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for updating bank account details.
 */
public interface UpdateAccountUseCase {
    Single<BankAccount> updateAccount(String id, BankAccount account);
}