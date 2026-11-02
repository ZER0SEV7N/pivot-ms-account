package com.pe.pivot.account.domain.port.out;

import com.pe.pivot.account.domain.model.BankAccount;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Output Port for bank account persistence operations in MongoDB
 */
public interface AccountRepositoryPort {
    Single<BankAccount> save(BankAccount account);
    Flowable<BankAccount> findAll();
    Maybe<BankAccount> findById(String id);
    Maybe<BankAccount> findByAccountNumberHash(String accountNumber);
    Maybe<BankAccount> findByCustomerId(String customerId);
    Single<Boolean> exitsByAccountNumberHash(String accountNumberHash);
    Completable deleteById(String id);
}
