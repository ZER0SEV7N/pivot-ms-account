package com.pe.pivot.account.domain.port.in;

import com.pe.pivot.account.domain.model.BankAccount;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for finding a bank account by its unique identifier.
 */
public interface FindAccountByIdUseCase {
    Single<BankAccount> findById(String id);
}
