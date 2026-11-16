package com.pe.pivot.account.infrastructure.adapter.out.persistence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.account.domain.model.AccountStatus;
import com.pe.pivot.account.domain.model.AccountType;
import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.entity.BankAccountDocument;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.mapper.AccountPersistenceMapper;
import com.pe.pivot.account.infrastructure.adapter.out.persistence.repository.ReactiveAccountMongoRepository;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Unit tests for AccountPersistenceAdapter.
 *
 * <p>Validates the reactive bridge between the RxJava 3 port and the Reactor MongoDB repository.
 */
class AccountPersistenceAdapterTest {

  private static final String ACCOUNT_ID = "account-1";
  private static final String ACCOUNT_HASH = "account-hash";

  private ReactiveAccountMongoRepository mongoRepository;
  private AccountPersistenceMapper mapper;
  private AccountPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    mongoRepository = mock(ReactiveAccountMongoRepository.class);
    mapper = mock(AccountPersistenceMapper.class);
    adapter = new AccountPersistenceAdapter(mongoRepository, mapper);
  }

  @Test
  @DisplayName("Should save a domain account and return the mapped result")
  void shouldSaveAccount() {
    // Arrange
    BankAccount account = domain();
    BankAccountDocument document = document();
    BankAccount saved = domain();
    when(mapper.toDocument(account)).thenReturn(document);
    when(mongoRepository.save(document)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(saved);

    // Act
    TestObserver<BankAccount> observer = adapter.save(account).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(saved);
    verify(mongoRepository).save(document);
  }

  @Test
  @DisplayName("Should stream all accounts from MongoDB")
  void shouldFindAllAccounts() {
    // Arrange
    BankAccountDocument document = document();
    BankAccount account = domain();
    when(mongoRepository.findAll()).thenReturn(Flux.just(document));
    when(mapper.toDomain(document)).thenReturn(account);

    // Act
    TestSubscriber<BankAccount> observer = adapter.findAll().test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
    observer.assertValueCount(1);
  }

  @Test
  @DisplayName("Should find an account by id")
  void shouldFindById() {
    // Arrange
    BankAccountDocument document = document();
    BankAccount account = domain();
    when(mongoRepository.findById(ACCOUNT_ID)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(account);

    // Act
    TestObserver<BankAccount> observer = adapter.findById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
  }

  @Test
  @DisplayName("Should complete without values when the account id does not exist")
  void shouldCompleteEmptyWhenIdDoesNotExist() {
    // Arrange
    when(mongoRepository.findById(ACCOUNT_ID)).thenReturn(Mono.empty());

    // Act
    TestObserver<BankAccount> observer = adapter.findById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertNoValues();
  }

  @Test
  @DisplayName("Should find an account by its hash")
  void shouldFindByAccountNumberHash() {
    // Arrange
    BankAccountDocument document = document();
    BankAccount account = domain();
    when(mongoRepository.findByAccountNumberHash(ACCOUNT_HASH)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(account);

    // Act
    TestObserver<BankAccount> observer = adapter.findByAccountNumberHash(ACCOUNT_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
  }

  @Test
  @DisplayName("Should stream accounts by customer id")
  void shouldFindByCustomerId() {
    // Arrange
    BankAccountDocument document = document();
    BankAccount account = domain();
    when(mongoRepository.findByCustomerId("customer-1")).thenReturn(Flux.just(document));
    when(mapper.toDomain(document)).thenReturn(account);

    // Act
    TestSubscriber<BankAccount> observer = adapter.findByCustomerId("customer-1").test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(account);
  }

  @Test
  @DisplayName("Should report whether an account hash exists")
  void shouldCheckAccountHashExistence() {
    // Arrange
    when(mongoRepository.existsByAccountNumberHash(ACCOUNT_HASH)).thenReturn(Mono.just(true));

    // Act
    TestObserver<Boolean> observer = adapter.existsByAccountNumberHash(ACCOUNT_HASH).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    observer.assertValue(true);
  }

  @Test
  @DisplayName("Should complete when deleting an account by id")
  void shouldDeleteById() {
    // Arrange
    when(mongoRepository.deleteById(ACCOUNT_ID)).thenReturn(Mono.empty());

    // Act
    TestObserver<Void> observer = adapter.deleteById(ACCOUNT_ID).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verify(mongoRepository).deleteById(ACCOUNT_ID);
  }

  @Test
  @DisplayName("Should propagate a MongoDB failure while saving")
  void shouldPropagateSaveFailure() {
    // Arrange
    BankAccount account = domain();
    BankAccountDocument document = document();
    when(mapper.toDocument(account)).thenReturn(document);
    when(mongoRepository.save(document))
        .thenReturn(Mono.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<BankAccount> observer = adapter.save(account).test();

    // Assert
    observer.assertNoValues();
    observer.assertError(IllegalStateException.class);
  }

  private BankAccount domain() {
    return BankAccount.builder()
        .id(ACCOUNT_ID)
        .accountNumber("191-00000000-0-01")
        .accountNumberHash(ACCOUNT_HASH)
        .customerId("customer-1")
        .accountType(AccountType.SAVINGS)
        .status(AccountStatus.ACTIVE)
        .build();
  }

  private BankAccountDocument document() {
    return BankAccountDocument.builder()
        .id(ACCOUNT_ID)
        .accountNumber("encrypted")
        .accountNumberHash(ACCOUNT_HASH)
        .customerId("customer-1")
        .accountType("SAVINGS")
        .status("ACTIVE")
        .build();
  }
}
