package com.pe.pivot.account.domain.port.in;

import com.pe.pivot.account.domain.model.BankAccount;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for opening a new bank account.
 */
public interface CreateAccountUseCase {
    Single<BankAccount> execute(BankAccount account);
}
