package com.pe.pivot.account.infrastructure.adapter.out.client.credit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Unit tests for CreditWebClientAdapter.
 *
 * <p>Validates active credit card detection and circuit breaker fallback behavior.
 */
class CreditWebClientAdapterTest {

  private ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  @BeforeEach
  void setUp() {
    circuitBreakerFactory = mock(ReactiveCircuitBreakerFactory.class);
    when(circuitBreakerFactory.create(anyString()))
        .thenReturn(new PassThroughCircuitBreaker());
  }

  @Test
  @DisplayName("Should report an active credit card when a matching active product exists")
  void shouldReportActiveCreditCard() {
    // Arrange
    String body = """
        [
          {
            "id": "credit-1",
            "customerId": "customer-1",
            "creditType": "PERSONAL_CREDIT_CARD",
            "status": "ACTIVE"
          }
        ]
        """;
    CreditWebClientAdapter adapter = adapter(jsonResponse(HttpStatus.OK, body));

    // Act
    TestObserver<Boolean> observer = adapter.hasActiveCreditCard("customer-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(true);
    verify(circuitBreakerFactory).create("creditServiceCircuitBreaker");
  }

  @Test
  @DisplayName("Should report no active credit card when the product is inactive")
  void shouldReportNoActiveCreditCardWhenInactive() {
    // Arrange
    String body = """
        [
          {
            "id": "credit-1",
            "customerId": "customer-1",
            "creditType": "PERSONAL_CREDIT_CARD",
            "status": "INACTIVE"
          }
        ]
        """;
    CreditWebClientAdapter adapter = adapter(jsonResponse(HttpStatus.OK, body));

    // Act
    TestObserver<Boolean> observer = adapter.hasActiveCreditCard("customer-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(false);
  }

  @Test
  @DisplayName("Should report no active credit card when the product list is empty")
  void shouldReportNoActiveCreditCardWhenListIsEmpty() {
    // Arrange
    CreditWebClientAdapter adapter = adapter(jsonResponse(HttpStatus.OK, "[]"));

    // Act
    TestObserver<Boolean> observer = adapter.hasActiveCreditCard("customer-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(false);
  }

  @Test
  @DisplayName("Should translate a communication failure into a BusinessRuleException")
  void shouldTranslateCommunicationFailure() {
    // Arrange
    CreditWebClientAdapter adapter = adapter(
        request -> Mono.error(new IllegalStateException("connection refused")));

    // Act
    TestObserver<Boolean> observer = adapter.hasActiveCreditCard("customer-1").test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("ms-credit"));
  }

  private CreditWebClientAdapter adapter(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:8083/api/v1/credits")
        .exchangeFunction(exchangeFunction)
        .build();
    return new CreditWebClientAdapter(webClient, circuitBreakerFactory);
  }

  private ExchangeFunction jsonResponse(HttpStatus status, String body) {
    return request -> Mono.just(ClientResponse.create(status)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(body)
        .build());
  }

  /**
   * Minimal pass-through circuit breaker that delegates to the supplied Mono or Flux.
   */
  private static final class PassThroughCircuitBreaker implements ReactiveCircuitBreaker {

    @Override
    public <T> Mono<T> run(Mono<T> toRun,
        java.util.function.Function<Throwable, Mono<T>> fallback) {
      return toRun.onErrorResume(fallback);
    }

    @Override
    public <T> Flux<T> run(Flux<T> toRun,
        java.util.function.Function<Throwable, Flux<T>> fallback) {
      return toRun.onErrorResume(fallback);
    }
  }
}
