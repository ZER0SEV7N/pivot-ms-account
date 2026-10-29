package com.pe.pivot.account.domain.port.in;

import io.reactivex.rxjava3.core.Completable;

/**
 * Input Port for deleting/closing a bank account.
 */
public interface DeleteAccountUseCase {
    Completable execute(String id);
}