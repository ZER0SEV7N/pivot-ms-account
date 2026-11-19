package com.pe.pivot.account.infrastructure.adapter.in.web.debit;

import com.pe.pivot.account.domain.port.in.debit.CreateDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.DeleteDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.PayWithDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.RetrieveDebitCardUseCase;
import com.pe.pivot.account.domain.port.in.debit.UpdateDebitCardUseCase;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.BalanceResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.mapper.AccountWebMapper;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardPaymentRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardPaymentResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardResponse;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.dto.DebitCardUpdateRequest;
import com.pe.pivot.account.infrastructure.adapter.in.web.debit.mapper.DebitCardWebMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * REST Controller implementing the OpenAPI contract for Debit Card operations with RxJava 3.
 */
@RestController
@RequestMapping("/api/v1/debit-cards")
@RequiredArgsConstructor
public class DebitCardController {

  private final CreateDebitCardUseCase createDebitCardUseCase;
  private final RetrieveDebitCardUseCase retrieveDebitCardUseCase;
  private final UpdateDebitCardUseCase updateDebitCardUseCase;
  private final DeleteDebitCardUseCase deleteDebitCardUseCase;
  private final PayWithDebitCardUseCase payWithDebitCardUseCase;
  private final DebitCardWebMapper mapper;
  private final AccountWebMapper accountWebMapper;

  /**
   * Creates a new debit card based on the provided DebitCardRequest.
   *
   * @POST /api/v1/debit-cards
   * @param request - the DebitCardRequest containing the details for the new debit card
   * @body : {
   *   "customerId": "string",
   *   "primaryAccountId": "string",
   *   "secondaryAccountIds": "string",
   * }
   * @return - Single emitting a DebitCardResponse with HTTP status 201 (Created)
   *
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Single<DebitCardResponse> createDebitCard(@Valid @RequestBody DebitCardRequest request) {
    return createDebitCardUseCase.create(mapper.toDomain(request))
            .map(mapper::toUnmaskedResponse);
  }

  /**
   * Retrieves all debit cards.
   *
   * @GET /api/v1/debits-cards
   * @return - Flowable emitting all DebitCardResponse objects
   */
  @GetMapping
  public Flowable<DebitCardResponse> getAllDebitCards() {
    return retrieveDebitCardUseCase.findAll().map(mapper::toResponse);
  }

  /**
   * Retrieves a debit card by its unique identifier.
   *
   * @GET /api/v1/debit-cards/{id}
   * @param id - the unique identifier of the debit card
   * @return - Single emitting a DebitCardResponse with HTTP status 200 (OK)
   */
  @GetMapping("/{id}")
  public Single<DebitCardResponse> getDebitCardById(@PathVariable String id) {
    return retrieveDebitCardUseCase.findById(id).map(mapper::toResponse);
  }

  /**
   * Retrieves a debit card by its card number.
   *
   * @GET /api/v1/debit-cards/number/{cardNumber}
   * @param cardNumber the card number of the debit card
   * @return Single emitting a DebitCardResponse with HTTP status 200 (OK)
   */
  @GetMapping("/number/{cardNumber}")
  public Single<DebitCardResponse> getDebitCardByNumber(@PathVariable String cardNumber) {
    return retrieveDebitCardUseCase.findByCardNumber(cardNumber).map(mapper::toResponse);
  }

  /**
   * Retrieves all debit cards associated with a specific customer ID.
   *
   * @GET /api/v1/debit-cards/customer/{customerId}
   * @param customerId the unique identifier of the customer
   * @return Flowable emitting all DebitCardResponse objects associated with the customer
   */
  @GetMapping("/customer/{customerId}")
  public Flowable<DebitCardResponse> getDebitCardsByCustomerId(@PathVariable String customerId) {
    return retrieveDebitCardUseCase.findByCustomerId(customerId).map(mapper::toResponse);
  }

  /**
   * Retrieves the primary account balance for a specific debit card by its unique identifier.
   *
   * @GET /api/v1/debit-cards/{id}/balance
   * @param id the unique identifier of the debit card
   * @return Single emitting a BalanceResponse with HTTP status 200 (OK)
   */
  @GetMapping("/{id}/balance")
  public Single<BalanceResponse> getDebitCardPrimaryBalance(@PathVariable String id) {
    return retrieveDebitCardUseCase.getPrimaryAccountBalance(id)
            .map(accountWebMapper::toBalanceResponse);
  }

  /**
   * Updates Debit Card information based on the provided DebitCardUpdateRequest.
   *
   * @PUT /api/v1/debit-cards/{id}
   * @param id the unique identifier of the debit card to update
   * @param request the DebitCardUpdateRequest containing the updated debit card information
   * @body : {
   *   "primaryAccountId": "string",
   *   "secondaryAccountIds": "string",
   *   "status": ACTIVE | BLOCKED | CLOSED
   * }
   * @return Single emitting a DebitCardResponse with HTTP status 200 (OK)
   */
  @PutMapping("/{id}")
  public Single<DebitCardResponse> updateDebitCard(
          @PathVariable String id, @Valid @RequestBody DebitCardUpdateRequest request) {
    return updateDebitCardUseCase.update(id, mapper.toDomain(request))
            .map(mapper::toResponse);
  }

  /**
   * Pays with a debit card by its unique identifier based on the provided DebitCardPaymentRequest.
   *
   * @PATCH /api/v1/debit-cards/{id}/pay
   * @param id the unique identifier of the debit card to use for payment
   * @param request the DebitCardPaymentRequest containing the payment amount
   * @body : {
   *   "amount": 100.00
   * }
   * @return Single emitting a DebitCardPaymentResponse with HTTP status 200 (OK)
   */
  @PatchMapping("/{id}/pay")
  public Single<DebitCardPaymentResponse> payWithDebitCard(
          @PathVariable String id, @Valid @RequestBody DebitCardPaymentRequest request) {
    return payWithDebitCardUseCase.pay(id, request.amount())
            .map(mapper::toPaymentResponse);
  }

  /**
   * Deletes a debit card by its unique identifier.
   *
   * @DELETE /api/v1/debit-cards/{id}
   * @param id the unique identifier of the debit card to delete
   * @return Completable indicating completion of the delete operation with HTTP status 204 (No Content)
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Completable deleteDebitCard(@PathVariable String id) {
    return deleteDebitCardUseCase.delete(id);
  }
}