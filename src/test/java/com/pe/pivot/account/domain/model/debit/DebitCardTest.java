package com.pe.pivot.account.domain.model.debit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebitCard}.
 */
@DisplayName("Should be able to manage debit card domain state")
class DebitCardTest {

  @Test
  @DisplayName("Should be active when the status is ACTIVE")
  void shouldBeActiveWhenStatusIsActive() {
    // (happy path)
    // Arrange
    DebitCard card = DebitCard.builder().status(DebitCardStatus.ACTIVE).build();

    // Act
    boolean active = card.isActive();

    // Assert
    assertThat(active).isTrue();
  }

  @Test
  @DisplayName("Should be inactive when the status is not ACTIVE")
  void shouldBeInactiveWhenStatusIsNotActive() {
    // (error: blocked or closed card)
    // Arrange
    DebitCard blocked = DebitCard.builder().status(DebitCardStatus.BLOCKED).build();
    DebitCard closed = DebitCard.builder().status(DebitCardStatus.CLOSED).build();
    DebitCard withoutStatus = DebitCard.builder().build();

    // Act / Assert
    assertThat(blocked.isActive()).isFalse();
    assertThat(closed.isActive()).isFalse();
    assertThat(withoutStatus.isActive()).isFalse();
  }

  @Test
  @DisplayName("Should be ordered with the primary account first and unique secondaries")
  void shouldBeOrderedWithPrimaryAccountFirstAndUniqueSecondaries() {
    // Arrange
    DebitCard card = DebitCard.builder()
            .primaryAccountId("acc-primary")
            .secondaryAccountIds(List.of("acc-2", "acc-3", "acc-2", "acc-primary"))
            .build();

    // Act
    List<String> ordered = card.getOrderedAccountIds();

    // Assert
    assertThat(ordered).containsExactly("acc-primary", "acc-2", "acc-3");
  }

  @Test
  @DisplayName("Should be ordered without a blank primary account")
  void shouldBeOrderedWithoutBlankPrimaryAccount() {
    // (error: missing primary account)
    // Arrange
    DebitCard card = DebitCard.builder()
            .primaryAccountId("   ")
            .secondaryAccountIds(List.of("acc-2"))
            .build();

    // Act
    List<String> ordered = card.getOrderedAccountIds();

    // Assert
    assertThat(ordered).containsExactly("acc-2");
  }

  @Test
  @DisplayName("Should be ordered with the primary only when secondaries are null")
  void shouldBeOrderedWithPrimaryOnlyWhenSecondariesAreNull() {
    // (error: missing secondary accounts)
    // Arrange
    DebitCard card = DebitCard.builder()
            .primaryAccountId("acc-primary")
            .secondaryAccountIds(null)
            .build();

    // Act
    List<String> ordered = card.getOrderedAccountIds();

    // Assert
    assertThat(ordered).containsExactly("acc-primary");
  }

  @Test
  @DisplayName("Should be ordered empty when no accounts are linked")
  void shouldBeOrderedEmptyWhenNoAccountsAreLinked() {
    // (error: no linked accounts)
    // Arrange
    DebitCard card = DebitCard.builder().build();

    // Act
    List<String> ordered = card.getOrderedAccountIds();

    // Assert
    assertThat(ordered).isEmpty();
  }

  @Test
  @DisplayName("Should be exposed with every lifecycle status")
  void shouldBeExposedWithEveryLifecycleStatus() {
    // Act
    DebitCardStatus[] statuses = DebitCardStatus.values();

    // Assert
    assertThat(statuses).containsExactly(
            DebitCardStatus.ACTIVE, DebitCardStatus.BLOCKED, DebitCardStatus.CLOSED);
  }
}
