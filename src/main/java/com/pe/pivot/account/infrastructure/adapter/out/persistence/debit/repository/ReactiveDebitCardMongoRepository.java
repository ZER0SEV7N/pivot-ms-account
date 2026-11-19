package com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.repository;

import com.pe.pivot.account.infrastructure.adapter.out.persistence.debit.entity.DebitCardDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Spring Data MongoDB repository for DebitCardDocument.
 */
public interface ReactiveDebitCardMongoRepository extends ReactiveMongoRepository<DebitCardDocument, String> {

  /**
   * Finds a DebitCardDocument by its cardNumberHash.
   *
   * @param cardNumberHash - the hashed card number to search for
   * @return - a Mono emitting the found DebitCardDocument, or empty if not found
   */
  Mono<DebitCardDocument> findByCardNumberHash(String cardNumberHash);

  /**
   * Finds all DebitCardDocuments associated with a given customerId.
   *
   * @param customerId - the unique identifier of the customer
   * @return - a Flux emitting all DebitCardDocuments for the specified customerId
   */
  Flux<DebitCardDocument> findByCustomerId(String customerId);

  /**
   * Checks if a DebitCardDocument exists with the specified cardNumberHash.
   *
   * @param cardNumberHash - the hashed card number to check for existence
   * @return - a Mono emitting true if a document exists, false otherwise
   */
  Mono<Boolean> existsByCardNumberHash(String cardNumberHash);
}
