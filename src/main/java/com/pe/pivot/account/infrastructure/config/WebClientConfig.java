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

     @Value("${application.clients.customer-service.url:http://localhost:8081/api/v1/customers}")
     private String customerServiceUrl;

     @Bean
     public WebClient customerWebClient(WebClient.Builder builder) {
         return builder
                 .baseUrl(customerServiceUrl)
                 .build();
     }
}
