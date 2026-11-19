package com.pe.pivot.account.infrastructure.adapter.in.web.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.exception.AccountNotFoundException;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.exception.DuplicateAccountException;
import com.pe.pivot.account.infrastructure.adapter.in.web.account.dto.ErrorResponse;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * <p>Validates the translation of domain and validation exceptions into standardized HTTP
 * responses.
 */
class GlobalExceptionHandlerTest {

  private static final String PATH = "/api/v1/accounts/account-1";

  private GlobalExceptionHandler handler;
  private ServerWebExchange exchange;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    exchange = exchange();
  }

  @Test
  @DisplayName("Should map AccountNotFoundException to HTTP 404")
  void shouldMapAccountNotFound() {
    // Act
    TestObserver<ResponseEntity<ErrorResponse>> observer = handler.handleNotFound(
        new AccountNotFoundException("account not found"), exchange).test();

    // Assert
    assertErrorResponse(observer, HttpStatus.NOT_FOUND, "account not found");
  }

  @Test
  @DisplayName("Should map CustomerNotFoundException to HTTP 404")
  void shouldMapCustomerNotFound() {
    // Act
    TestObserver<ResponseEntity<ErrorResponse>> observer = handler.handleNotFound(
        new CustomerNotFoundException("customer not found"), exchange).test();

    // Assert
    assertErrorResponse(observer, HttpStatus.NOT_FOUND, "customer not found");
  }

  @Test
  @DisplayName("Should map DuplicateAccountException to HTTP 409")
  void shouldMapDuplicateAccount() {
    // Act
    TestObserver<ResponseEntity<ErrorResponse>> observer = handler.handleDuplicate(
        new DuplicateAccountException("duplicate account"), exchange).test();

    // Assert
    assertErrorResponse(observer, HttpStatus.CONFLICT, "duplicate account");
  }

  @Test
  @DisplayName("Should map BusinessRuleException to HTTP 400")
  void shouldMapBusinessRule() {
    // Act
    TestObserver<ResponseEntity<ErrorResponse>> observer = handler.handleBusinessRule(
        new BusinessRuleException("business rule violated"), exchange).test();

    // Assert
    assertErrorResponse(observer, HttpStatus.BAD_REQUEST, "business rule violated");
  }

  @Test
  @DisplayName("Should map payload validation errors to HTTP 400 with joined messages")
  void shouldMapValidationErrors() {
    // Arrange
    WebExchangeBindException exception = mock(WebExchangeBindException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getAllErrors()).thenReturn(List.of(
        new ObjectError("request", "customerId is required"),
        new ObjectError("request", "accountType is required")));

    // Act
    TestObserver<ResponseEntity<ErrorResponse>> observer = handler
        .handleValidationErrors(exception, exchange).test();

    // Assert
    assertErrorResponse(observer, HttpStatus.BAD_REQUEST,
        "customerId is required; accountType is required");
  }

  @Test
  @DisplayName("Should map unexpected exceptions to HTTP 500 without leaking details")
  void shouldMapUnexpectedException() {
    // Act
    TestObserver<ResponseEntity<ErrorResponse>> observer = handler.handleGenericException(
        new IllegalStateException("sensitive detail"), exchange).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> {
      assertThat(response.getStatusCode().value())
          .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().message())
          .isEqualTo("An unexpected internal server error occurred.");
      return true;
    });
  }

  private void assertErrorResponse(TestObserver<ResponseEntity<ErrorResponse>> observer,
      HttpStatus expectedStatus, String expectedMessage) {
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(response -> {
      assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus.value());
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().status()).isEqualTo(expectedStatus.value());
      assertThat(response.getBody().error()).isEqualTo(expectedStatus.getReasonPhrase());
      assertThat(response.getBody().message()).isEqualTo(expectedMessage);
      assertThat(response.getBody().path()).isEqualTo(PATH);
      assertThat(response.getBody().timestamp()).isNotNull();
      return true;
    });
  }

  private ServerWebExchange exchange() {
    RequestPath path = mock(RequestPath.class);
    when(path.value()).thenReturn(PATH);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    when(request.getPath()).thenReturn(path);
    ServerWebExchange serverWebExchange = mock(ServerWebExchange.class);
    when(serverWebExchange.getRequest()).thenReturn(request);
    return serverWebExchange;
  }
}
