package com.pe.pivot.account.infrastructure.adapter.out.persistence.mapper;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.entity.BankAccountDocument;
import com.pe.pivot.account.infrastructure.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper converting between Domain BankAccount and MongoDB BankAccountDocument.
 */
@Component
@RequiredArgsConstructor
public class AccountPersistenceMapper {

  private final CryptoUtil cryptoUtil;

  /**
   * Maps a BankAccount domain object to a BankAccountDocument for persistence in MongoDB.
   *
   * @param domain - The BankAccount domain object to be converted.
   * @return - The corresponding BankAccountDocument for MongoDB storage.
   */
  public BankAccountDocument toDocument(BankAccount domain) {
    return BankAccountDocument.builder()
        .id(domain.getId())
        .accountNumber(cryptoUtil.encryptAes256(domain.getAccountNumber()))
        .accountNumberHash(domain.getAccountNumberHash())
        .customerId(domain.getCustomerId())
        .accountType(
            domain.getAccountType() != null ? domain.getAccountType().name() : null)
        .currency(domain.getCurrency())
        .balance(domain.getBalance())
        .maintenanceFee(domain.getMaintenanceFee())
        .minimumDailyAverage(domain.getMinimumDailyAverage())
        .minOpeningAmount(domain.getMinOpeningAmount())
        .maxMonthlyMovements(domain.getMaxMonthlyMovements())
        .maxFreeMonthlyTransactions(domain.getMaxFreeMonthlyTransactions())
        .transactionCommissionFee(domain.getTransactionCommissionFee())
        .lastAppliedCommission(domain.getLastAppliedCommission())
        .currentMonthlyMovements(domain.getCurrentMonthlyMovements())
        .allowedMovementDay(domain.getAllowedMovementDay())
        .holders(domain.getHolders())
        .authorizedSigners(domain.getAuthorizedSigners())
        .status(domain.getStatus() != null ? domain.getStatus().name() : null)
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  /**
   * Maps a BankAccountDocument from MongoDB to a BankAccount domain object.
   *
   * @param doc - The BankAccountDocument retrieved from MongoDB.
   * @return - The corresponding BankAccount domain object.
   */
  public BankAccount toDomain(BankAccountDocument doc) {
    return BankAccount.builder()
        .id(doc.getId())
        .accountNumber(cryptoUtil.decryptAes256(doc.getAccountNumber()))
        .accountNumberHash(doc.getAccountNumberHash())
        .customerId(doc.getCustomerId())
        .accountType(
            doc.getAccountType() != null ? AccountType.valueOf(doc.getAccountType()) : null)
        .currency(doc.getCurrency())
        .balance(doc.getBalance())
        .maintenanceFee(doc.getMaintenanceFee())
        .minimumDailyAverage(doc.getMinimumDailyAverage())
        .minOpeningAmount(doc.getMinOpeningAmount())
        .maxMonthlyMovements(doc.getMaxMonthlyMovements())
        .maxFreeMonthlyTransactions(doc.getMaxFreeMonthlyTransactions())
        .transactionCommissionFee(doc.getTransactionCommissionFee())
        .lastAppliedCommission(doc.getLastAppliedCommission())
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
