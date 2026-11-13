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

    /**
     * The maintenance fee for CHECKING accounts.
     */
    private Integer savingsMaxMonthlyMovements = 5;
    private BigDecimal checkingDefaultMaintenanceFee = new BigDecimal("15.00");
    private Integer fixedTermDefaultMovementDay = 28;
    private BigDecimal defaultMinOpeningAmount = BigDecimal.ZERO;
    private BigDecimal vipMinOpeningAmount = new BigDecimal("500.00");
    private BigDecimal vipMinimumDailyAverage = new BigDecimal("500.00");
    private Integer maxFreeMonthlyTransactions = 3;
    private BigDecimal defaultTransactionCommission = new BigDecimal("5.00");
}