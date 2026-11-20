package com.pe.pivot.account.infrastructure.adapter.out.client.credit;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.port.out.CreditClientPort;
import com.pe.pivot.account.infrastructure.adapter.out.client.credit.dto.CreditSummaryDto;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

/**
 * Output Adapter communicating reactively with ms-credit (:8083) protected by Resilience4j
 * Circuit Breaker (2s timeout).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditWebClientAdapter implements CreditClientPort {

  private static final String CREDIT_CUSTOMER_URI = "/customer/{customerId}";
  private static final String CIRCUIT_BREAKER_NAME = "creditServiceCircuitBreaker";
  private static final String SERVICE_UNAVAILABLE_MSG =
          "Credit verification service (ms-credit) is currently unavailable or timed out (2s).";

  private final WebClient creditWebClient;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  /**
   * Checks if a customer has an active credit card by calling the ms-credit service.
   *
   * @param customerId - The unique identifier of the customer to check for an active credit card.
   * @return - Single emitting true if the customer has an active credit card, false otherwise.
   * @throws BusinessRuleException if the ms-credit service is unavailable or times out.
   */
  @Override
  public Single<Boolean> hasActiveCreditCard(String customerId) {
    log.debug("Checking active credit card in ms-credit for customerId: {}", customerId);
    return evaluateCreditPredicate(customerId, CreditSummaryDto::isActiveCreditCard);
  }

  @Override
  public Single<Boolean> hasOverdueDebt(String customerId) {
    log.debug("Checking overdue credit debt in ms-credit for customerId: {}", customerId);
    return evaluateCreditPredicate(customerId, CreditSummaryDto::hasOverdueDebt);
  }

  /**
   * Fetches customer credits from ms-credit and evaluates the given predicate under Circuit
   * Breaker protection.
   *
   * @param customerId the unique identifier of the customer
   * @param predicate the condition to evaluate across the customer's credit products
   * @return Single emitting true if any credit matches the predicate
   */
  private Single<Boolean> evaluateCreditPredicate(
          String customerId, Predicate<CreditSummaryDto> predicate) {
    // Fetch customer credits from ms-credit and evaluate the predicate under Circuit Breaker
    // protection.
    Mono<Boolean> mono = creditWebClient.get()
            .uri(CREDIT_CUSTOMER_URI, customerId)
            .retrieve()
            .bodyToFlux(CreditSummaryDto.class)
            .any(predicate)
            // Apply Circuit Breaker to handle ms-credit service failures or timeouts.
            .transform(it -> circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME)
                    .run(it, throwable -> {
                      log.error("Circuit breaker triggered calling ms-credit for customerId {}: {}",
                              customerId, throwable.getMessage());
                      return Mono.error(new BusinessRuleException(SERVICE_UNAVAILABLE_MSG));
                    }));

    return RxJava3Adapter.monoToSingle(mono);
  }

}
