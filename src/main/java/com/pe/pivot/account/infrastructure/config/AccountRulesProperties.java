package com.pe.pivot.account.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Externalized business rule parameters for bank accounts fetched from ms-config.
 * Prevents hardocded product parameters in the application layer.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.account-rules")
public class AccountRulesProperties {

    private BigDecimal checkingMaintenanceFee;
    private Integer savingsMaxMonthlyMovements;
    private Integer fixedTermMaxMonthlyMovements;
    private BigDecimal zeroMaintenanceFee;
}
