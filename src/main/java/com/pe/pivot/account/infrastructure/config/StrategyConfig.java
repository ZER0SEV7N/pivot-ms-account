package com.pe.pivot.account.infrastructure.config;

import com.pe.pivot.account.domain.strategy.AccountValidationStrategy;
import com.pe.pivot.account.domain.strategy.BusinessAccountStrategy;
import com.pe.pivot.account.domain.strategy.PersonalAccountStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class registering account validation strategies.
 */
@Configuration
public class StrategyConfig {

    @Bean
    public AccountValidationStrategy personalAccountStrategy() {
        return new PersonalAccountStrategy();
    }

    @Bean
    public AccountValidationStrategy businessAccountStrategy() {
        return new BusinessAccountStrategy();
    }
}
