package com.pe.pivot.account.domain.port.in;

import com.pe.pivot.account.domain.model.BankAccount;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for querying bank accounts and balances.
 */
public interface RetrieveAccountUseCase {
    Flowable<BankAccount> findAll();
    Single<BankAccount> findById(String id);
    Single<BankAccount> findByAccountNumber(String accountNumber);
    Flowable<BankAccount> findByCustomerId(String customerId);
}