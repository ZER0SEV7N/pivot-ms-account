package com.pe.pivot.account.infrastructure.adapter.out.persistence.repository;

import com.pe.pivot.account.infrastructure.adapter.out.persistence.entity.BankAccountDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data Reactive MongoDB repository for BankAccountDocument using derived query methods.
 * Avoids @Query annotation per project specification.
 */
@Repository
public interface ReactiveAccountMongoRepository extends ReactiveMongoRepository<BankAccountDocument, String> {

    Mono<BankAccountDocument> findByAccountNumberHash(String accountNumberHash);

    Flux<BankAccountDocument> findByCustomerId(String customerId);

    Mono<Boolean> existsByAccountNumberHash(String accountNumberHash);
}
