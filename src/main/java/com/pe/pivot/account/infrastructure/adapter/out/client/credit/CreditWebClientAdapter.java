package com.pe.pivot.account.infrastructure.adapter.out.client.credit;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.port.out.CreditClientPort;
import com.pe.pivot.account.infrastructure.adapter.out.client.credit.dto.CreditSummaryDto;
import io.reactivex.rxjava3.core.Single;
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

    // Create a Mono to call the ms-credit service and check for an active credit card
    Mono<Boolean> mono = creditWebClient.get()
        .uri("/customer/{customerId}", customerId)
        .retrieve()
        .bodyToFlux(CreditSummaryDto.class)
        .any(CreditSummaryDto::isActiveCreditCard)
        .transform(it -> circuitBreakerFactory.create("creditServiceCircuitBreaker")
            .run(it, throwable -> {
              log.error("Circuit breaker triggered calling ms-credit for customerId {}: {}",
                  customerId, throwable.getMessage());
              return Mono.error(new BusinessRuleException(
                  "Credit verification service (ms-credit) is currently unavailable or "
                      + "timed out (2s)."));
            }));

    return RxJava3Adapter.monoToSingle(mono);
  }
}
