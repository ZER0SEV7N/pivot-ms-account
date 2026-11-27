package com.pe.pivot.account.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for reactive WebClient communication with external microservices.
 */
@Configuration
public class WebClientConfig {

  // Base URL for the customer service, injected from application properties or localhost.
  @Value("${application.clients.customer-service.url:http://localhost:8081/api/v1/customers}")
  private String customerServiceUrl;

  // Value = "${application.clients.credit-service.url:http://localhost:8083/api/v1/credits}"
  @Value("${application.clients.credit-service.url:http://localhost:8083/api/v1/credits}")
  private String creditServiceUrl;

  /**
   * Creates a WebClient bean for communicating with the customer service.
   *
   * @return - A configured WebClient instance for the customer service.
   */
  @Bean
  public WebClient customerWebClient(WebClient.Builder builder) {
    return builder.clone()
        .baseUrl(customerServiceUrl)
        .build();
  }

  /**
   * Creates a WebClient bean for communicating with the credit service.
   *
   * @return - A configured WebClient instance for the credit service.
   */
  @Bean
  public WebClient creditWebClient(WebClient.Builder builder) {
    return builder.clone()
        .baseUrl(creditServiceUrl)
        .build();
  }
}
