package com.pe.pivot.account.infrastructure.adapter.out.persistence.mapper;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.entity.BankAccountDocument;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper converting between Domain BankAccount and MongoDB BankAccountDocument
 */
@Component
@RequiredArgsConstructor
public class AccountPersistenceMapper {

    private final CryptoUtil cryptoUtil;

    public BankAccountDocument toDocument(BankAccount domain) {
        return BankAccountDocument.builder()
                .id(domain.getId())
                .accountNumber(cryptoUtil.encryptAes256(domain.getAccountNumber()))
                .accountNumberHash(domain.getAccountNumberHash())
                .customerId(domain.getCustomerId())
                .accountType(domain.getAccountType() != null ? domain.getAccountType().name() : null)
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .maintenanceFee(domain.getMaintenanceFee())
                .maxMonthlyMovements(domain.getMaxMonthlyMovements())
                .currentMonthlyMovements(domain.getCurrentMonthlyMovements())
                .allowedMovementDay(domain.getAllowedMovementDay())
                .holders(domain.getHolders())
                .authorizedSigners(domain.getAuthorizedSigners())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public BankAccount toDomain(BankAccountDocument doc) {
        return BankAccount.builder()
                .id(doc.getId())
                .accountNumber(cryptoUtil.decryptAes256(doc.getAccountNumber()))
                .accountNumberHash(doc.getAccountNumberHash())
                .customerId(doc.getCustomerId())
                .accountType(doc.getAccountType() != null ? AccountType.valueOf(doc.getAccountType()) : null)
                .currency(doc.getCurrency())
                .balance(doc.getBalance())
                .maintenanceFee(doc.getMaintenanceFee())
                .maxMonthlyMovements(doc.getMaxMonthlyMovements())
                .currentMonthlyMovements(doc.getCurrentMonthlyMovements())
                .allowedMovementDay(doc.getAllowedMovementDay())
                .holders(doc.getHolders())
                .authorizedSigners(doc.getAuthorizedSigners())
                .status(doc.getStatus() != null ? AccountStatus.valueOf(doc.getStatus()) : null)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
