package com.pe.pivot.account.infrastructure.config;

import com.pe.pivot.account.domain.model.debit.DebitCard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Reactive Redis configuration for caching DebitCard metadata in Spring Boot 4.
 */
@Configuration
public class RedisConfig {

  /**
   * Creates a ReactiveRedisTemplate for DebitCard objects, configuring key and value serializers.
   *
   * @param factory the ReactiveRedisConnectionFactory to create connections to Redis
   * @return a ReactiveRedisTemplate configured for DebitCard objects
   */
  @Bean
  public ReactiveRedisTemplate<String, DebitCard> debitCardRedisTemplate(
          ReactiveRedisConnectionFactory factory) {

    // Configure key and value serializers for RedisTemplate
    StringRedisSerializer keySerializer = new StringRedisSerializer();
    JacksonJsonRedisSerializer<DebitCard> valueSerializer =
            new JacksonJsonRedisSerializer<>(DebitCard.class);

    // Configure the RedisSerializationContext with key and value serializers
    RedisSerializationContext<String, DebitCard> context =
            RedisSerializationContext.<String, DebitCard>newSerializationContext(keySerializer)
                    .value(valueSerializer)
                    .hashKey(keySerializer)
                    .hashValue(valueSerializer)
                    .build();

    return new ReactiveRedisTemplate<>(factory, context);
  }

}
