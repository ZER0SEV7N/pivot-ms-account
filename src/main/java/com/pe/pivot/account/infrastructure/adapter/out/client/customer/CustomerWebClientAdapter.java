package com.pe.pivot.account.infrastructure.adapter.out.client.customer;

import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.infrastructure.adapter.out.client.customer.dto.CustomerClientResponse;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerWebClientAdapter implements CustomerClientPort {

    private final WebClient customerWebClient;

    /**
     * Finds a customer by their unique identifier by calling the ms-customer service.
     *
     * @param customerId - the unique identifier of the customer
     * @return - Maybe emitting the CustomerSummary if found, or empty if not found
     */
    @Override
    public Maybe<CustomerSummary> findCustomerById(String customerId) {
      log.debug("Calling ms-customer GET /{} for customerId: {}", customerId, customerId);

      Mono<CustomerSummary> mono = customerWebClient.get()
              .uri("/{id}", customerId)
              .retrieve()
              .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
              .bodyToMono(CustomerClientResponse.class)
              .map(this::toDomain)
              .transform(it -> circuitBreakerFactory.create("customerServiceCircuitBreaker")
                      .run(it, throwable -> {
                        log.error("Circuit breaker triggered calling ms-customer for customerId {}: {}",
                                customerId, throwable.getMessage());
                        return Mono.error(new BusinessRuleException(
                                "Customer service (ms-customer) is currently unavailable or timed out (2s)."));
                      }));

      return RxJava3Adapter.monoToMaybe(mono);
    }

  private CustomerSummary toDomain(CustomerClientResponse dto) {
    return CustomerSummary.builder()
            .id(dto.id())
            .customerType(dto.customerType())
            .profile(dto.profile())
            .documentNumber(dto.documentNumber())
            .status(dto.status())
            .build();
  }
}
