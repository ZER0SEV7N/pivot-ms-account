package com.pe.pivot.account.application.support.debitcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pe.pivot.account.application.support.accounts.AccountBalancePolicy;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.model.debit.DebitCardPaymentResult;
import com.pe.pivot.account.domain.model.debit.DebitedAccountEntry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CascadingDebitPolicy}.
 */
@DisplayName("Should be able to cascade a debit across linked accounts")
class CascadingDebitPolicyTest {

  private static final String CARD_NUMBER = "4557891234567890";

  private CascadingDebitPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new CascadingDebitPolicy(new AccountBalancePolicy());
  }

  @Test
  @DisplayName("Should be debited entirely from a single account with enough funds")
  void shouldBeDebitedEntirelyFromSingleAccountWithEnoughFunds() {
    // (happy path)
    // Arrange
    DebitCard card = card();
    BankAccount account = account("acc-1", "500.00");

    // Act
    CascadingExecution execution = policy.execute(card, List.of(account),
            new BigDecimal("100.00"));

    // Assert
    DebitCardPaymentResult result = execution.result();
    assertThat(execution.modifiedAccounts()).containsExactly(account);
    assertThat(result.debitCardId()).isEqualTo("dc-1");
    assertThat(result.maskedCardNumber()).isEqualTo("************7890");
    assertThat(result.totalAmount()).isEqualByComparingTo("100.00");
    assertThat(result.totalCommission()).isEqualByComparingTo("0");
    assertThat(result.debitedAccounts()).hasSize(1);
    DebitedAccountEntry entry = result.debitedAccounts().get(0);
    assertThat(entry.accountId()).isEqualTo("acc-1");
    assertThat(entry.previousBalance()).isEqualByComparingTo("500.00");
    assertThat(entry.newBalance()).isEqualByComparingTo("400.00");
    assertThat(entry.debitedAmount()).isEqualByComparingTo("100.00");
    assertThat(result.processedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should be cascaded across accounts in priority order")
  void shouldBeCascadedAcrossAccountsInPriorityOrder() {
    // Arrange
    DebitCard card = card();
    BankAccount primary = account("acc-1", "30.00");
    BankAccount secondary = account("acc-2", "100.00");

    // Act
    CascadingExecution execution = policy.execute(card, List.of(primary, secondary),
            new BigDecimal("100.00"));

    // Assert
    assertThat(primary.getBalance()).isEqualByComparingTo("0");
    assertThat(secondary.getBalance()).isEqualByComparingTo("30.00");
    assertThat(execution.result().debitedAccounts())
            .extracting(DebitedAccountEntry::accountId)
            .containsExactly("acc-1", "acc-2");
    assertThat(execution.result().debitedAccounts().get(0).debitedAmount())
            .isEqualByComparingTo("30.00");
    assertThat(execution.result().debitedAccounts().get(1).debitedAmount())
            .isEqualByComparingTo("70.00");
  }

  @Test
  @DisplayName("Should be skipped for a non ACTIVE account")
  void shouldBeSkippedForNonActiveAccount() {
    // (error: blocked primary account)
    // Arrange
    DebitCard card = card();
    BankAccount blocked = account("acc-1", "100.00");
    blocked.setStatus(AccountStatus.BLOCKED);
    BankAccount active = account("acc-2", "100.00");

    // Act
    CascadingExecution execution = policy.execute(card, List.of(blocked, active),
            new BigDecimal("50.00"));

    // Assert
    assertThat(blocked.getBalance()).isEqualByComparingTo("100.00");
    assertThat(active.getBalance()).isEqualByComparingTo("50.00");
    assertThat(execution.result().debitedAccounts())
            .extracting(DebitedAccountEntry::accountId)
            .containsExactly("acc-2");
  }

  @Test
  @DisplayName("Should be skipped for an account without spendable balance")
  void shouldBeSkippedForAccountWithoutSpendableBalance() {
    // (error: empty primary account)
    // Arrange
    DebitCard card = card();
    BankAccount empty = account("acc-1", "0.00");
    BankAccount funded = account("acc-2", "80.00");

    // Act
    CascadingExecution execution = policy.execute(card, List.of(empty, funded),
            new BigDecimal("50.00"));

    // Assert
    assertThat(empty.getBalance()).isEqualByComparingTo("0");
    assertThat(funded.getBalance()).isEqualByComparingTo("30.00");
    assertThat(execution.result().debitedAccounts()).hasSize(1);
  }

  @Test
  @DisplayName("Should be skipped for an account with a null balance")
  void shouldBeSkippedForAccountWithNullBalance() {
    // (error: missing balance)
    // Arrange
    DebitCard card = card();
    BankAccount noBalance = account("acc-1", null);
    BankAccount funded = account("acc-2", "60.00");

    // Act
    CascadingExecution execution = policy.execute(card, List.of(noBalance, funded),
            new BigDecimal("40.00"));

    // Assert
    assertThat(funded.getBalance()).isEqualByComparingTo("20.00");
    assertThat(execution.result().debitedAccounts()).hasSize(1);
  }

  @Test
  @DisplayName("Should be rejected when the combined funds are insufficient")
  void shouldBeRejectedWhenCombinedFundsAreInsufficient() {
    // (error: insufficient combined funds)
    // Arrange
    DebitCard card = card();
    BankAccount first = account("acc-1", "20.00");
    BankAccount second = account("acc-2", "30.00");

    // Act / Assert
    assertThatThrownBy(() -> policy.execute(card, List.of(first, second),
            new BigDecimal("100.00")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient combined funds");
  }

  @Test
  @DisplayName("Should be applied with the commission when the free quota is exhausted")
  void shouldBeAppliedWithCommissionWhenFreeQuotaIsExhausted() {
    // Arrange
    DebitCard card = card();
    BankAccount account = account("acc-1", "100.00");
    account.setMaxFreeMonthlyTransactions(0);
    account.setCurrentMonthlyMovements(0);
    account.setTransactionCommissionFee(new BigDecimal("5.00"));

    // Act
    CascadingExecution execution = policy.execute(card, List.of(account),
            new BigDecimal("50.00"));

    // Assert
    assertThat(execution.result().totalCommission()).isEqualByComparingTo("5.00");
    assertThat(execution.result().debitedAccounts().get(0).appliedCommission())
            .isEqualByComparingTo("5.00");
    assertThat(account.getBalance()).isEqualByComparingTo("45.00");
  }

  @Test
  @DisplayName("Should be applied without commission when the fee is null")
  void shouldBeAppliedWithoutCommissionWhenFeeIsNull() {
    // (error: missing commission fee)
    // Arrange
    DebitCard card = card();
    BankAccount account = account("acc-1", "100.00");
    account.setMaxFreeMonthlyTransactions(0);
    account.setCurrentMonthlyMovements(0);
    account.setTransactionCommissionFee(null);

    // Act
    CascadingExecution execution = policy.execute(card, List.of(account),
            new BigDecimal("50.00"));

    // Assert
    assertThat(execution.result().totalCommission()).isEqualByComparingTo("0");
    assertThat(account.getBalance()).isEqualByComparingTo("50.00");
  }

  @Test
  @DisplayName("Should be stopped once the amount is fully covered")
  void shouldBeStoppedOnceAmountIsFullyCovered() {
    // Arrange
    DebitCard card = card();
    BankAccount primary = account("acc-1", "500.00");
    BankAccount secondary = account("acc-2", "500.00");

    // Act
    CascadingExecution execution = policy.execute(card, List.of(primary, secondary),
            new BigDecimal("100.00"));

    // Assert
    assertThat(secondary.getBalance()).isEqualByComparingTo("500.00");
    assertThat(execution.modifiedAccounts()).containsExactly(primary);
    assertThat(execution.result().debitedAccounts()).hasSize(1);
  }

  private DebitCard card() {
    return DebitCard.builder()
            .id("dc-1")
            .cardNumber(CARD_NUMBER)
            .build();
  }

  private BankAccount account(String id, String balance) {
    return BankAccount.builder()
            .id(id)
            .accountNumber("191-" + id + "-0-01")
            .accountType(AccountType.SAVINGS)
            .status(AccountStatus.ACTIVE)
            .balance(balance == null ? null : new BigDecimal(balance))
            .build();
  }
}
