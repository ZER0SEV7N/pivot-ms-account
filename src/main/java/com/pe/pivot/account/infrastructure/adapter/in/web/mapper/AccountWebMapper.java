// src/main/java/com/pe/pivot/account/infrastructure/adapter/in/web/mapper/AccountWebMapper.java
package com.pe.pivot.account.infrastructure.adapter.in.web.mapper;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.in.web.dto.AccountRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.dto.BalanceResponse;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Web Mapper converting between immutable Java 17 Record DTOs and Domain BankAccount.
 * Enforces data masking on all read/update responses while revealing full account number only upon creation.
 */
@Component
public class AccountWebMapper {

    public BankAccount toDomain(AccountRequest request) {
        return BankAccount.builder()
                .accountNumber(request.accountNumber())
                .customerId(request.customerId())
                .accountType(request.accountType())
                .currency(request.currency())
                .balance(request.balance())
                .maintenanceFee(request.maintenanceFee())
                .maxMonthlyMovements(request.maxMonthlyMovements())
                .allowedMovementDay(request.allowedMovementDay())
                .holders(request.holders())
                .authorizedSigners(request.authorizedSigners())
                .status(request.status())
                .build();
    }

    /**
     * Used ONLY during account creation (POST) so the client receives the generated account number once.
     */
    public AccountResponse toUnmaskedResponse(BankAccount domain) {
        return buildAccountResponse(domain, domain.getAccountNumber());
    }

    /**
     * Used for all GET, PUT, and PATCH operations, returning the masked account number (****0-01).
     */
    public AccountResponse toResponse(BankAccount domain) {
        return buildAccountResponse(domain, DataMaskingUtil.maskAccountNumber(domain.getAccountNumber()));
    }

    public BalanceResponse toBalanceResponse(BankAccount domain) {
        return BalanceResponse.builder()
                .accountId(domain.getId())
                .accountNumber(DataMaskingUtil.maskAccountNumber(domain.getAccountNumber()))
                .accountType(domain.getAccountType())
                .currency(domain.getCurrency())
                .availableBalance(domain.getBalance())
                .currentMonthlyMovements(domain.getCurrentMonthlyMovements())
                .maxMonthlyMovements(domain.getMaxMonthlyMovements())
                .queriedAt(Instant.now())
                .build();
    }

    private AccountResponse buildAccountResponse(BankAccount domain, String formattedAccountNumber) {
        return AccountResponse.builder()
                .id(domain.getId())
                .accountNumber(formattedAccountNumber)
                .customerId(domain.getCustomerId())
                .accountType(domain.getAccountType())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .maintenanceFee(domain.getMaintenanceFee())
                .maxMonthlyMovements(domain.getMaxMonthlyMovements())
                .currentMonthlyMovements(domain.getCurrentMonthlyMovements())
                .allowedMovementDay(domain.getAllowedMovementDay())
                .holders(domain.getHolders())
                .authorizedSigners(domain.getAuthorizedSigners())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}