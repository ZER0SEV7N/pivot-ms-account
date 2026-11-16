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
public interface ReactiveAccountMongoRepository
    extends ReactiveMongoRepository<BankAccountDocument, String> {

  /**
   * Finds a BankAccountDocument by its accountNumberHash.
   *
   * @param accountNumberHash - the hashed account number to search for
   * @return - a Mono emitting the found BankAccountDocument, or empty if not found
   */
  Mono<BankAccountDocument> findByAccountNumberHash(String accountNumberHash);

  /**
   * Finds all BankAccountDocuments associated with a given customerId.
   *
   * @param customerId - the unique identifier of the customer
   * @return - a Flux emitting all BankAccountDocuments for the specified customerId
   */
  Flux<BankAccountDocument> findByCustomerId(String customerId);

  /**
   * Checks if a BankAccountDocument exists with the specified accountNumberHash.
   *
   * @param accountNumberHash - the hashed account number to check for existence
   * @return - a Mono emitting true if a document exists, false otherwise
   */
  Mono<Boolean> existsByAccountNumberHash(String accountNumberHash);
}
