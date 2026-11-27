package com.pe.pivot.account.infrastructure.config;

import com.mongodb.observability.ObservabilitySettings;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the MongoDB driver's native Micrometer observability, so every find/insert/update/delete
 * executed by the synchronous or reactive driver becomes a span exported to Grafana Tempo.
 *
 * <p>Driver 5.7+ provides {@link ObservabilitySettings}; this is the recommended mechanism and
 * replaces Spring Data's deprecated context provider/listener APIs.
 */
@Configuration
public class MongoTracingConfig {

  /**
   * Attaches the Micrometer observation registry to the auto-configured MongoClient.
   *
   * @param observationRegistry Micrometer registry provided by Spring Boot
   * @return a customizer that enables driver-native MongoDB tracing
   */
  @Bean
  public MongoClientSettingsBuilderCustomizer mongoObservationCustomizer(
      ObservationRegistry observationRegistry) {
    return builder ->
        builder.observabilitySettings(
            ObservabilitySettings.micrometerBuilder()
                .observationRegistry(observationRegistry)
                .build());
  }
}