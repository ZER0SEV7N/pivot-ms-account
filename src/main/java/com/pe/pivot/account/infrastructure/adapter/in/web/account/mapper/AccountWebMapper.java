package com.pe.pivot.account.infrastructure.adapter.in.web.account.mapper;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceResponse;
import com.pe.pivot.account.infrastructure.util.DataMaskingUtil;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Web Mapper converting between immutable Java 17 Record DTOs and Domain BankAccount.
 * Enforces data masking on all read/update responses while revealing full account number only
 * upon creation.
 */
@Component
public class AccountWebMapper {

  /**
   * Converts an AccountRequest DTO into a BankAccount domain object.
   *
   * @param request - the request DTO to convert
   * @return - the corresponding BankAccount domain object
   */
  public BankAccount toDomain(AccountRequest request) {
    return BankAccount.builder()
        .accountNumber(request.accountNumber())
        .customerId(request.customerId())
        .accountType(request.accountType())
        .currency(request.currency())
        .balance(request.balance())
        .maintenanceFee(request.maintenanceFee())
        .minimumDailyAverage(request.minimumDailyAverage())
        .minOpeningAmount(request.minOpeningAmount())
        .maxMonthlyMovements(request.maxMonthlyMovements())
        .maxFreeMonthlyTransactions(request.maxFreeMonthlyTransactions())
        .transactionCommissionFee(request.transactionCommissionFee())
        .allowedMovementDay(request.allowedMovementDay())
        .holders(request.holders())
        .authorizedSigners(request.authorizedSigners())
        .status(request.status())
        .build();
  }

  /**
   * Converts a BankAccount into an unmasked AccountResponse DTO.
   *
   * @param domain - the bank account to convert
   * @return - the unmasked response DTO
   */
  public AccountResponse toUnmaskedResponse(BankAccount domain) {
    return buildAccountResponse(domain, domain.getAccountNumber());
  }

  /**
   * Converts a BankAccount into a masked AccountResponse DTO.
   *
   * @param domain - the bank account to convert
   * @return - the response DTO with a masked account number
   */
  public AccountResponse toResponse(BankAccount domain) {
    return buildAccountResponse(domain,
        DataMaskingUtil.maskAccountNumber(domain.getAccountNumber()));
  }

  /**
   * Converts a BankAccount into a BalanceResponse DTO.
   *
   * @param domain - the bank account to convert
   * @return - the balance response DTO
   */
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

  private AccountResponse buildAccountResponse(BankAccount domain,
      String formattedAccountNumber) {
    return AccountResponse.builder()
        .id(domain.getId())
        .accountNumber(formattedAccountNumber)
        .customerId(domain.getCustomerId())
        .accountType(domain.getAccountType())
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
        .status(domain.getStatus())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }
}
