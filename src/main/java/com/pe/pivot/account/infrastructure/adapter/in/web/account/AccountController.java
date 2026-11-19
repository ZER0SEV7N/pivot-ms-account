package com.pe.pivot.account.infrastructure.adapter.in.web.account;

import com.pe.pivot.account.domain.port.in.CreateAccountUseCase;
import com.pe.pivot.account.domain.port.in.DeleteAccountUseCase;
import com.pe.pivot.account.domain.port.in.RetrieveAccountUseCase;
import com.pe.pivot.account.domain.port.in.UpdateAccountUseCase;
import com.pe.pivot.account.domain.port.in.UpdateBalanceUseCase;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.AccountResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceUpdateRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.mapper.AccountWebMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller implementing the OpenAPI contract for Bank Account operations with RxJava 3.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

  private final CreateAccountUseCase createAccountUseCase;
  private final RetrieveAccountUseCase retrieveAccountUseCase;
  private final UpdateAccountUseCase updateAccountUseCase;
  private final UpdateBalanceUseCase updateBalanceUseCase;
  private final DeleteAccountUseCase deleteAccountUseCase;
  private final AccountWebMapper mapper;

  /**
   * Creates a new bank account based on the provided AccountRequest.
   *
   * @Post - /api/v1/accounts
   * @param request - the AccountRequest containing the details for the new account
   * @return - Single emitting a ResponseEntity containing the AccountResponse with HTTP status
   *     201 (Created)
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Single<ResponseEntity<AccountResponse>> createAccount(
      @Valid @RequestBody AccountRequest request) {
    return createAccountUseCase.create(mapper.toDomain(request))
        .map(mapper::toUnmaskedResponse)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  /**
   * Retrieves all bank accounts.
   *
   * @Get - /api/v1/accounts
   * @return - Flowable emitting AccountResponse objects
   */
  @GetMapping
  public Flowable<AccountResponse> getAllAccounts() {
    return retrieveAccountUseCase.findAll()
        .map(mapper::toResponse);
  }

  /**
   * Retrieves a bank account by its unique identifier.
   *
   * @Get - /api/v1/accounts/{id}
   * @param id - the unique identifier of the bank account
   * @return - Single emitting a ResponseEntity containing the AccountResponse with HTTP status
   *     200 (OK)
   */
  @GetMapping("/{id}")
  public Single<ResponseEntity<AccountResponse>> getAccountById(@PathVariable String id) {
    return retrieveAccountUseCase.findById(id)
        .map(mapper::toResponse)
        .map(ResponseEntity::ok);
  }

  /**
   * Retrieves a bank account by its account number.
   *
   * @Get - /api/v1/accounts/number/{accountNumber}
   * @param accountNumber - the account number of the bank account
   * @return - Single emitting a ResponseEntity containing the AccountResponse with HTTP status
   *     200 (OK)
   */
  @GetMapping("/number/{accountNumber}")
  public Single<ResponseEntity<AccountResponse>> getAccountByNumber(
      @PathVariable String accountNumber) {
    return retrieveAccountUseCase.findByAccountNumber(accountNumber)
        .map(mapper::toResponse)
        .map(ResponseEntity::ok);
  }

  /**
   * Retrieves all bank accounts for a given customer.
   *
   * @Get - /api/v1/accounts/customer/{customerId}
   * @param customerId - the unique identifier of the customer
   * @return - Flowable emitting AccountResponse objects
   */
  @GetMapping("/customer/{customerId}")
  public Flowable<AccountResponse> getAccountsByCustomerId(@PathVariable String customerId) {
    return retrieveAccountUseCase.findByCustomerId(customerId)
        .map(mapper::toResponse);
  }

  /**
   * Updates a bank account by its unique identifier.
   *
   * @Put - /api/v1/accounts/{id}
   * @param id - the unique identifier of the bank account
   * @param request - the AccountRequest containing the updated account information
   * @return - Single emitting a ResponseEntity containing the AccountResponse with HTTP status
   *     200 (OK)
   */
  @PutMapping("/{id}")
  public Single<ResponseEntity<AccountResponse>> updateAccount(@PathVariable String id,
      @Valid @RequestBody AccountRequest request) {
    return updateAccountUseCase.updateAccount(id, mapper.toDomain(request))
        .map(mapper::toResponse)
        .map(ResponseEntity::ok);
  }

  /**
   * Updates the balance of a bank account based on the provided BalanceUpdateRequest.
   *
   * @Patch - /api/v1/accounts/{id}/balance
   * @param id - the unique identifier of the bank account
   * @param request - the BalanceUpdateRequest containing the operation type and amount
   * @return - Single emitting a ResponseEntity containing the AccountResponse with HTTP status
   *     200 (OK)
   */
  @PatchMapping("/{id}/balance")
  public Single<ResponseEntity<AccountResponse>> updateAccountBalance(@PathVariable String id,
      @Valid @RequestBody BalanceUpdateRequest request) {
    return updateBalanceUseCase.updateBalance(id, request.operationType(), request.amount())
        .map(mapper::toResponse)
        .map(ResponseEntity::ok);
  }

  /**
   * Consults the available balance of a bank account.
   *
   * @Get /api/v1/accounts/{id}/balance
   * @param id - the unique identifier of the bank account
   * @return - Single emitting a BalanceResponse with the available balance
   */
  @GetMapping("/{id}/balance")
  public Single<BalanceResponse> getAccountBalance(@PathVariable String id) {
    return retrieveAccountUseCase.findById(id)
        .map(mapper::toBalanceResponse);
  }

  /**
   * Deletes a bank account by its unique identifier.
   *
   * @Delete - /api/v1/accounts/{id}
   * @param id - the unique identifier of the bank account
   * @return - Completable completing with HTTP status 204 (No Content) if successful
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Completable deleteAccount(@PathVariable String id) {
    return deleteAccountUseCase.delete(id);
  }
}
