package com.pe.pivot.account.infrastructure.adapter.in.web.mapper;

import org.springframework.stereotype.Component;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.in.web.dto.AccountRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.dto.BalanceResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Web Mapper converting between immutable Java 17 Record DTOs and Domain BankAccount.
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

    public AccountResponse toResponse(BankAccount domain) {
        return AccountResponse.builder()
                .id(domain.getId())
                .accountNumber(domain.getAccountNumber())
                .accountNumberHash(domain.getAccountNumberHash())
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

    public BalanceResponse toBalanceResponse(BankAccount domain) {
        return BalanceResponse.builder()
                .accountId(domain.getId())
                .accountNumber(domain.getAccountNumber())
                .accountType(domain.getAccountType())
                .currency(domain.getCurrency())
                .availableBalance(domain.getBalance())
                .currentMonthlyMovements(domain.getCurrentMonthlyMovements())
                .maxMonthlyMovements(domain.getMaxMonthlyMovements())
                .queriedAt(Instant.now())
                .build();
    }
}