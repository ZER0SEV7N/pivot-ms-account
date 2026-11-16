package com.pe.pivot.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CustomerSummary value object helpers.
 */
class CustomerSummaryTest {

  @Test
  @DisplayName("Should identify personal customers ignoring case")
  void shouldIdentifyPersonalCustomer() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().customerType("personal").build();

    // Act / Assert
    assertThat(customer.isPersonal()).isTrue();
    assertThat(customer.isBusiness()).isFalse();
  }

  @Test
  @DisplayName("Should identify business customers ignoring case")
  void shouldIdentifyBusinessCustomer() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().customerType("Business").build();

    // Act / Assert
    assertThat(customer.isBusiness()).isTrue();
    assertThat(customer.isPersonal()).isFalse();
  }

  @Test
  @DisplayName("Should return false for null customer types")
  void shouldReturnFalseForNullCustomerType() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().build();

    // Act / Assert
    assertThat(customer.isPersonal()).isFalse();
    assertThat(customer.isBusiness()).isFalse();
  }

  @Test
  @DisplayName("Should identify active customers ignoring case")
  void shouldIdentifyActiveCustomer() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().status("active").build();

    // Act / Assert
    assertThat(customer.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should identify inactive customers")
  void shouldIdentifyInactiveCustomer() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().status("BLOCKED").build();

    // Act / Assert
    assertThat(customer.isActive()).isFalse();
  }

  @Test
  @DisplayName("Should identify VIP profiles ignoring case")
  void shouldIdentifyVipProfile() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().profile("vip").build();

    // Act / Assert
    assertThat(customer.isVip()).isTrue();
    assertThat(customer.isPyme()).isFalse();
  }

  @Test
  @DisplayName("Should identify PYME profiles ignoring case")
  void shouldIdentifyPymeProfile() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().profile("Pyme").build();

    // Act / Assert
    assertThat(customer.isPyme()).isTrue();
    assertThat(customer.isVip()).isFalse();
  }

  @Test
  @DisplayName("Should return false for null profile")
  void shouldReturnFalseForNullProfile() {
    // Arrange
    CustomerSummary customer = CustomerSummary.builder().build();

    // Act / Assert
    assertThat(customer.isVip()).isFalse();
    assertThat(customer.isPyme()).isFalse();
  }
}
