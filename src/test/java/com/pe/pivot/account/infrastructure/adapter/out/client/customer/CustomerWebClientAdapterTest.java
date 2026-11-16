package com.pe.pivot.account.infrastructure.adapter.out.client.customer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.CustomerSummary;
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
 * Unit tests for CustomerWebClientAdapter.
 *
 * <p>Uses a stubbed WebClient exchange function and a pass-through circuit breaker to validate
 * successful responses, empty 4xx responses, and fallback behavior.
 */
class CustomerWebClientAdapterTest {

  private ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  @BeforeEach
  void setUp() {
    circuitBreakerFactory = mock(ReactiveCircuitBreakerFactory.class);
    when(circuitBreakerFactory.create(anyString()))
        .thenReturn(new PassThroughCircuitBreaker());
  }

  @Test
  @DisplayName("Should map a successful customer response into a CustomerSummary")
  void shouldMapSuccessfulResponse() {
    // Arrange
    String body = """
        {
          "id": "customer-1",
          "customerType": "PERSONAL",
          "profile": "VIP",
          "documentType": "DNI",
          "documentNumber": "12345678",
          "status": "ACTIVE"
        }
        """;
    CustomerWebClientAdapter adapter = adapter(jsonResponse(HttpStatus.OK, body));

    // Act
    TestObserver<CustomerSummary> observer = adapter.findCustomerById("customer-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValueCount(1);
    observer.assertValue(customer -> "customer-1".equals(customer.getId())
        && "PERSONAL".equals(customer.getCustomerType())
        && "VIP".equals(customer.getProfile())
        && "12345678".equals(customer.getDocumentNumber())
        && customer.isActive());
    verify(circuitBreakerFactory).create("customerServiceCircuitBreaker");
  }

  @Test
  @DisplayName("Should complete empty when ms-customer answers with a 4xx status")
  void shouldCompleteEmptyOnClientError() {
    // Arrange
    CustomerWebClientAdapter adapter = adapter(
        request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build()));

    // Act
    TestObserver<CustomerSummary> observer = adapter.findCustomerById("missing").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertNoValues();
  }

  @Test
  @DisplayName("Should translate a communication failure into a BusinessRuleException")
  void shouldTranslateCommunicationFailure() {
    // Arrange
    CustomerWebClientAdapter adapter = adapter(
        request -> Mono.error(new IllegalStateException("connection refused")));

    // Act
    TestObserver<CustomerSummary> observer = adapter.findCustomerById("customer-1").test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("ms-customer"));
  }

  private CustomerWebClientAdapter adapter(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:8081/api/v1/customers")
        .exchangeFunction(exchangeFunction)
        .build();
    return new CustomerWebClientAdapter(webClient, circuitBreakerFactory);
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
