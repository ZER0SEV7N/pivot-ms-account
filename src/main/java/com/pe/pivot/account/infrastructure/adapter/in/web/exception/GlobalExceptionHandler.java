package com.pe.pivot.account.infrastructure.adapter.in.web.exception;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.exception.DuplicateAccountException;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global Exception Handler translating domain and validation exceptions into OpenAPI ErrorResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles exceptions related to resources not found, such as AccountNotFoundException and CustomerNotFoundException.
     *
     * @param ex - the exception that was thrown
     * @param exchange - the current server web exchange
     * @return - Single emitting a ResponseEntity
     * containing the ErrorResponse with HTTP status 404 (Not Found)
     */
    @ExceptionHandler({AccountNotFoundException.class, CustomerNotFoundException.class})
    public Single<ResponseEntity<ErrorResponse>> handleNotFound(RuntimeException ex, ServerWebExchange exchange) {
        log.warn("Resource not found: {}", ex.getMessage());
        return Single.just(buildResponse(ex, HttpStatus.NOT_FOUND, ex.getMessage(), exchange));
    }

    /**
     * Handles exceptions related to duplicate resources, such as DuplicateAccountException.
     *
     * @param ex - the exception that was thrown
     * @param exchange - the current server web exchange
     * @return - Single emitting a ResponseEntity
     * containing the ErrorResponse with HTTP status 409 (Conflict)
     */
    @ExceptionHandler(DuplicateAccountException.class)
    public Single<ResponseEntity<ErrorResponse>> handleDuplicate(DuplicateAccountException ex, ServerWebExchange exchange) {
        log.warn("Conflict error: {}", ex.getMessage());
        return Single.just(buildResponse(ex, HttpStatus.CONFLICT, ex.getMessage(), exchange));
    }

    /**
     * Handles exceptions related to business rule violations, such as BusinessRuleException.
     *
     * @param ex - the exception that was thrown
     * @param exchange - the current server web exchange
     * @return - Single emitting a ResponseEntity
     * containing the ErrorResponse with HTTP status 400 (Bad Request)
     */
    @ExceptionHandler(BusinessRuleException.class)
    public Single<ResponseEntity<ErrorResponse>> handleBusinessRule(BusinessRuleException ex, ServerWebExchange exchange) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return Single.just(buildResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage(), exchange));
    }

    /**
     * Handles validation errors that occur when binding request payloads to DTOs, such as WebExchangeBindException.
     *
     * @param ex - the exception that was thrown
     * @param exchange - the current server web exchange
     * @return - Single emitting a ResponseEntity
     * containing the ErrorResponse with HTTP status 400 (Bad Request)
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Single<ResponseEntity<ErrorResponse>> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        String errors = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Payload validation error: {}", errors);
        return Single.just(buildResponse(ex, HttpStatus.BAD_REQUEST, errors, exchange));
    }

    /**
     * Handles any unexpected exceptions that are not explicitly handled by other methods.
     *
     * @param ex - the exception that was thrown
     * @param exchange - the current server web exchange
     * @return - Single emitting a ResponseEntity
     * containing the ErrorResponse with HTTP status 500 (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public Single<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error occurred: ", ex);
        return Single.just(buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred.", exchange));
    }

    /**
     * Builds a standardized ErrorResponse and wraps it in a ResponseEntity.
     *
     * @param ex - the exception to attach to the ErrorResponse
     * @param status - the HTTP status to be returned
     * @param message - the error message to be included in the response
     * @param exchange - the current server web exchange, used to extract the request path
     * @return - ResponseEntity containing the ErrorResponse with the specified HTTP status
     */
    private ResponseEntity<ErrorResponse> buildResponse(Throwable ex, HttpStatus status, String message,
                                                       ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder(ex, status, message)
                .title(status.getReasonPhrase())
                .property("timestamp", Instant.now())
                .property("error", status.getReasonPhrase())
                .property("path", exchange.getRequest().getPath().value())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}
