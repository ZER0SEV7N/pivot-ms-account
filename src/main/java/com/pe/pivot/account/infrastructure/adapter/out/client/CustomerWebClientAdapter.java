package com.pe.pivot.account.infrastructure.adapter.out.client;

import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.infrastructure.adapter.out.client.dto.CustomerClientResponse;
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
        log.debug("Calling ms.customer GET /{id} for customerId: {}", customerId);

        // Create a Mono to call the ms-customer service and retrieve the customer information
        Mono<CustomerSummary> mono = customerWebClient.get()
                .uri("/{id}", customerId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
                .bodyToMono(CustomerClientResponse.class)
                .map(res -> CustomerSummary.builder()
                        .id(res.id())
                        .customerType(res.customerType())
                        .profile(res.profile())
                        .documentNumber(res.documentNumber())
                        .status(res.status())
                        .build())
                .onErrorResume(ex -> {
                    log.warn("Customer lookup returned empty or error for ID {}: {}", customerId, ex.getMessage());
                    return Mono.empty();
                });
        return RxJava3Adapter.monoToMaybe(mono);
    }
}
