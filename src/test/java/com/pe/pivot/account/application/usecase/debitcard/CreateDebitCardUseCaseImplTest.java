package com.pe.pivot.account.application.usecase.debitcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.application.support.debitcard.DebitCardDomainInitializer;
import com.pe.pivot.account.domain.exception.BusinessRuleException;
import com.pe.pivot.account.domain.exception.CustomerNotFoundException;
import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.model.CustomerSummary;
import com.pe.pivot.account.domain.model.debit.DebitCard;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.out.CreditClientPort;
import com.pe.pivot.account.domain.port.out.CustomerClientPort;
import com.pe.pivot.account.domain.port.out.DebitCardRepositoryPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CreateDebitCardUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to issue a debit card")
class CreateDebitCardUseCaseImplTest {

  private static final String CUSTOMER_ID = "cust-1";
  private static final String PRIMARY_ACCOUNT = "acc-1";

  @Mock
  private DebitCardRepositoryPort debitCardRepository;

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private CreditClientPort creditClient;

  @Mock
  private FindAccountByIdUseCase findAccountByIdUseCase;

  @Mock
  private DebitCardDomainInitializer initializer;

  private CreateDebitCardUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateDebitCardUseCaseImpl(debitCardRepository, customerClient, creditClient,
            findAccountByIdUseCase, initializer);
  }

  @Test
  @DisplayName("Should be created when the customer and linked accounts are eligible")
  void shouldBeCreatedWhenCustomerAndLinkedAccountsAreEligible() {
    // (happy path)
    // Arrange
    DebitCard card = card();
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(activeCustomer()));
    when(creditClient.hasOverdueDebt(CUSTOMER_ID)).thenReturn(Single.just(false));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT))
            .thenReturn(Single.just(account(PRIMARY_ACCOUNT, CUSTOMER_ID,
                    AccountStatus.ACTIVE)));
    when(debitCardRepository.save(card)).thenReturn(Single.just(card));

    // Act
    TestObserver<DebitCard> observer = useCase.create(card).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(card);
    verify(initializer).initialize(any(CustomerSummary.class), any(DebitCard.class));
    verify(debitCardRepository).save(card);
  }

  @Test
  @DisplayName("Should be rejected when the customer does not exist")
  void shouldBeRejectedWhenCustomerDoesNotExist() {
    // (error: customer not found)
    // Arrange
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<DebitCard> observer = useCase.create(card()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(CustomerNotFoundException.class);
    verifyNoInteractions(creditClient);
    verify(initializer, never()).initialize(any(), any());
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when the customer is inactive")
  void shouldBeRejectedWhenCustomerIsInactive() {
    // (error: inactive customer)
    // Arrange
    CustomerSummary inactive = activeCustomer();
    inactive.setStatus("INACTIVE");
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.just(inactive));

    // Act
    TestObserver<DebitCard> observer = useCase.create(card()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("inactive or blocked"));
    verifyNoInteractions(creditClient);
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when the customer has an overdue credit debt")
  void shouldBeRejectedWhenCustomerHasOverdueCreditDebt() {
    // (error: overdue credit debt)
    // Arrange
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(activeCustomer()));
    when(creditClient.hasOverdueDebt(CUSTOMER_ID)).thenReturn(Single.just(true));

    // Act
    TestObserver<DebitCard> observer = useCase.create(card()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("overdue credit debt"));
    verify(initializer, never()).initialize(any(), any());
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when a linked account belongs to another customer")
  void shouldBeRejectedWhenLinkedAccountBelongsToAnotherCustomer() {
    // (error: account ownership mismatch)
    // Arrange
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(activeCustomer()));
    when(creditClient.hasOverdueDebt(CUSTOMER_ID)).thenReturn(Single.just(false));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT))
            .thenReturn(Single.just(account(PRIMARY_ACCOUNT, "other-cust",
                    AccountStatus.ACTIVE)));

    // Act
    TestObserver<DebitCard> observer = useCase.create(card()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("does not belong"));
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when a linked account is not ACTIVE")
  void shouldBeRejectedWhenLinkedAccountIsNotActive() {
    // (error: inactive linked account)
    // Arrange
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(activeCustomer()));
    when(creditClient.hasOverdueDebt(CUSTOMER_ID)).thenReturn(Single.just(false));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT))
            .thenReturn(Single.just(account(PRIMARY_ACCOUNT, CUSTOMER_ID,
                    AccountStatus.BLOCKED)));

    // Act
    TestObserver<DebitCard> observer = useCase.create(card()).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> error.getMessage().contains("non-ACTIVE"));
    verify(debitCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be propagated when the debit card persistence fails")
  void shouldBePropagatedWhenDebitCardPersistenceFails() {
    // (error: persistence failure)
    // Arrange
    DebitCard card = card();
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(activeCustomer()));
    when(creditClient.hasOverdueDebt(CUSTOMER_ID)).thenReturn(Single.just(false));
    when(findAccountByIdUseCase.findById(PRIMARY_ACCOUNT))
            .thenReturn(Single.just(account(PRIMARY_ACCOUNT, CUSTOMER_ID,
                    AccountStatus.ACTIVE)));
    when(debitCardRepository.save(card))
            .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<DebitCard> observer = useCase.create(card).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
  }

  private DebitCard card() {
    return DebitCard.builder()
            .customerId(CUSTOMER_ID)
            .primaryAccountId(PRIMARY_ACCOUNT)
            .secondaryAccountIds(List.of())
            .build();
  }

  private CustomerSummary activeCustomer() {
    return CustomerSummary.builder()
            .id(CUSTOMER_ID)
            .customerType("PERSONAL")
            .status("ACTIVE")
            .firstName("Ada")
            .lastName("Lovelace")
            .build();
  }

  private BankAccount account(String id, String customerId, AccountStatus status) {
    return BankAccount.builder()
            .id(id)
            .customerId(customerId)
            .status(status)
            .build();
  }
}
