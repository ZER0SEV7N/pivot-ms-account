package com.pe.pivot.account.infrastructure.adapter.out.persistence;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.mapper.AccountPersistenceMapper;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.repository.ReactiveAccountMongoRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * Persistence Adapter implementing AccountRepositoryPort with Reactive MongoDB and RxJava 3.
 */
@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final ReactiveAccountMongoRepository mongoRepository;
    private final AccountPersistenceMapper mapper;

    /**
     * Saves a BankAccount to the MongoDB database.
     *
     * @param account - the BankAccount domain object to be saved
     * @return - Single emitting the saved BankAccount domain object
     */
    @Override
    public Single<BankAccount> save (BankAccount account) {
        return RxJava3Adapter.monoToSingle(
                mongoRepository.save(mapper.toDocument(account))
                        .map(mapper::toDomain)
        );
    }

    /**
     * Retrieves all BankAccount records from the MongoDB database.
     *
     * @return - Flowable emitting all BankAccount domain objects
     */
    @Override
    public Flowable<BankAccount> findAll() {
        return RxJava3Adapter.fluxToFlowable(
                mongoRepository.findAll().map(mapper::toDomain)
        );
    }

    /**
     * Finds a BankAccount by its unique identifier.
     *
     * @param id - the unique identifier of the BankAccount
     * @return - Maybe emitting the found BankAccount or empty if not found
     */
    @Override
    public Maybe<BankAccount> findById(String id) {
        return RxJava3Adapter.monoToMaybe(
                mongoRepository.findById(id).map(mapper::toDomain)
        );
    }

    /**
     * Finds a BankAccount by its hashed account number.
     *
     * @param accountNumberHash - the hashed account number of the BankAccount
     * @return - Maybe emitting the found BankAccount or empty if not found
     */
    @Override
    public Maybe<BankAccount> findByAccountNumberHash(String accountNumberHash) {
        return RxJava3Adapter.monoToMaybe(
                mongoRepository.findByAccountNumberHash(accountNumberHash)
                        .map(mapper::toDomain)
        );
    }

    /**
     * Finds all BankAccounts associated with a specific customer ID.
     *
     * @param customerId - the unique identifier of the customer
     * @return - Flowable emitting all BankAccount domain objects associated with the customer
     */
    @Override
    public Flowable<BankAccount> findByCustomerId(String customerId) {
        return RxJava3Adapter.fluxToFlowable(
                mongoRepository.findByCustomerId(customerId)
                        .map(mapper::toDomain)
        );
    }

    /**
     * Checks if a BankAccount exists by its hashed account number.
     *
     * @param accountNumberHash - the hashed account number of the BankAccount
     * @return - Single emitting true if the BankAccount exists, false otherwise
     */
    @Override
    public Single<Boolean> existsByAccountNumberHash(String accountNumberHash) {
        return RxJava3Adapter.monoToSingle(
                mongoRepository.existsByAccountNumberHash(accountNumberHash)
        );
    }

    /**
     * Deletes a BankAccount by its unique identifier.
     *
     * @param id - the unique identifier of the BankAccount
     * @return - Completable emitting when the deletion is complete
     */
    @Override
    public Completable deleteById(String id) {
        return RxJava3Adapter.monoToCompletable(
                mongoRepository.deleteById(id)
        );
    }
}
