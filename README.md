# Pivot Banking System - Account & Debit Card Microservice (`pivot-ms-account`)

## Overview
Reactive microservice (`:8082`) responsible for managing **Passive Bank Products** (**Savings**, **Checking**, and **Fixed-Term** accounts) and **Debit Cards** (Phase III). It validates customer profiles (`STANDARD`, `VIP`, `PYME`) against `ms-customer` (`:8081`), verifies active Credit Card eligibility and **blocks new product acquisition when a customer holds `OVERDUE` debt** via `ms-credit` (`:8083`) using **Resilience4j Circuit Breakers (2s timeout)**, executes **cascading multi-account debit card payments/withdrawals**, accelerates debit card lookups with **Reactive Redis Cache-Aside**, and participates in the **Yanki Kafka Saga** strictly against a card's `primaryAccountId`.

## Architecture, Design Patterns & Quality Gates
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) with strict separation of `domain`, `application`, and `infrastructure` layers, developed **API-First** from `account-api.yaml`.
- **Reactive Stack:** Java 17 + Spring Boot `4.1.2` + Spring Cloud `2025.1.4` + Spring WebFlux (Netty) + **RxJava 3 (`3.1.12`)** (`Single`, `Maybe`, `Flowable`, `Completable`) + `reactor-adapter` + Reactive Spring Data MongoDB + **Reactive Spring Data Redis** + **Spring Boot Starter Kafka** + Netflix Eureka Client + **Resilience4j**.
- **SOLID & Design Patterns (Zero God Classes):**
  - **Single Responsibility Principle (SRP):** Pure reactive orchestrators for accounts (`CreateAccountUseCaseImpl`, `RetrieveAccountUseCaseImpl`, `FindAccountByIdUseCaseImpl`, `UpdateAccountUseCaseImpl`, `BalanceUseCaseImpl`, `DeleteAccountUseCaseImpl`) and debit cards (issuance, account linking, primary account updates, balance lookup, cascading debit execution).
  - **Domain Support Separation (`application/support/`):** `AccountDomainInitializer`, `AccountBalancePolicy`, `CascadingDebitPolicy`, `CascadingExecution`, and `DebitCardAccountValidator`.
  - **Strategy Pattern (OCP):** `PersonalAccountStrategy` (`VIP` active credit card rule) and `BusinessAccountStrategy` (`CHECKING`-only and `PYME` zero-maintenance-fee rule), plus `OVERDUE` debt blocking via `CreditClientPort`.
  - **Decorator Pattern (OCP) for Redis Caching (Phase III):** `CachedDebitCardRepositoryAdapter` (`@Primary`) wraps MongoDB debit card persistence transparently for Cache-Aside reads and write-through/eviction updates.
  - **Event-Driven Saga Pattern (Phase III):** Consumes `yanki.payment.requested`, debits/credits strictly each linked debit card's `primaryAccountId` with compensating rollback on partial failure, and publishes `yanki.payment.result`.
  - **Circuit Breaker Pattern (`Resilience4j`):** Protects outbound reactive calls in `CustomerWebClientAdapter` and `CreditWebClientAdapter` with a **2-second timeout**.
- **Code Quality & Testing:**
  - **Google Java Style (`maven-checkstyle-plugin`):** Enforced during Maven `validate` phase.
  - **Unit Testing (`JUnit 5` + `Mockito` + `RxJava 3 TestObserver` + `StepVerifier`):** AAA pattern and FIRST principles achieving **~99.4% instruction coverage** verified by **JaCoCo (`0.8.15`)** (minimum gate: 90%).

## Security & Secrets (`HashiCorp Vault`)
- **Vault Path:** `secret/pivot-bank`
- **Required Secret Key:** `aes-secret-key` (AES-256-GCM encryption at rest and SHA-256 blind indexing for `accountNumber` and debit `cardNumber`).
- **Mask-on-Read Presentation:** Full `accountNumber` and `cardNumber` are returned **only once** upon creation. Subsequent reads mask sensitive digits via `DataMaskingUtil`.
- **Soft Delete:** `DELETE /accounts/{id}` performs logical closure (`status = CLOSED`) only when `balance == 0.00` (requires `ROLE_ADMIN` via Gateway).

## API Endpoints

### Bank Accounts (`/api/v1/accounts`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/accounts` | Open a new bank account (blocks customers with `OVERDUE` debt; validates `VIP`/`PYME` rules & `minOpeningAmount`; enriches `customerName`) |
| `GET` | `/api/v1/accounts` | Retrieve all bank accounts (masked numbers) |
| `GET` | `/api/v1/accounts/{id}` | Retrieve bank account by ID |
| `GET` | `/api/v1/accounts/number/{accountNumber}` | Retrieve bank account by number via SHA-256 blind index |
| `GET` | `/api/v1/accounts/customer/{customerId}` | Retrieve all bank accounts belonging to a customer |
| `PUT` | `/api/v1/accounts/{id}` | Update account holders, authorized signers, or status |
| `GET` | `/api/v1/accounts/{id}/balance` | Consult available balance and monthly movements counter |
| `PATCH` | `/api/v1/accounts/{id}/balance` | Process `DEPOSIT` or `WITHDRAWAL` (applies automatic commission after free limit) |
| `DELETE` | `/api/v1/accounts/{id}` | Soft delete (`status = CLOSED`) if `balance == 0.00` |

### Debit Cards (`/api/v1/debit-cards` - Phase III)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/debit-cards` | Issue a new Debit Card linked to a customer's primary account and optional ordered secondary accounts |
| `GET` | `/api/v1/debit-cards` | Retrieve all debit cards (masked card numbers) |
| `GET` | `/api/v1/debit-cards/{id}` | Retrieve debit card by ID (Redis Cache-Aside) |
| `GET` | `/api/v1/debit-cards/customer/{customerId}` | Retrieve all debit cards belonging to a customer |
| `GET` | `/api/v1/debit-cards/{id}/primary-balance` | Consult the available balance of the debit card's `primaryAccountId` |
| `PATCH` | `/api/v1/debit-cards/{id}/accounts` | Associate additional bank accounts in priority order |
| `PATCH` | `/api/v1/debit-cards/{id}/primary-account` | Change the debit card's designated `primaryAccountId` |
| `POST` | `/api/v1/debit-cards/{id}/pay` | Execute a cascading debit card payment/withdrawal across linked accounts in priority order |
| `DELETE` | `/api/v1/debit-cards/{id}` | Logically close/cancel a debit card |

## Inter-Service Communication, Kafka Saga & Observability
- **`pivot-ms-config` (`:8888`):** Fetches externalized rules (`AccountRulesProperties`) and Kafka/Redis settings.
- **`pivot-ms-eureka` (`:8761`):** Registers service instance as `MS-ACCOUNT`.
- **`pivot-ms-customer` (`:8081`):** Validated via `WebClient` + `Resilience4j` (2s timeout) for customer status, profile (`VIP`/`PYME`), and `customerName` display enrichment.
- **`pivot-ms-credit` (`:8083`):** Validated via `WebClient` + `Resilience4j` (2s timeout) for active credit cards (`VIP`/`PYME`) and `OVERDUE` debt blocking.
- **Apache Kafka (`:9092`):** Consumes `yanki.payment.requested` from `ms-yanki`, settles strictly on `primaryAccountId` (with compensating rollback if credit fails), and publishes `yanki.payment.result` for `ms-yanki` and `ms-transaction`.
- **Consumed by:** `pivot-ms-transaction` (`:8084`) for account operations, transfers, and debit card payments/withdrawals.
- **Observability:** Exposes `/actuator/health/liveness` & `/actuator/health/readiness` and ships structured logs via **Promtail -> Loki -> Grafana (`:3000`)**.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7.0 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`, configured via `spring.mongodb.uri`).
- **Database Name:** `db_pivot_account`
- **Collections:** `accounts`, `debit_cards`

## How to Run (Local, Docker & Kubernetes)
1. Ensure MongoDB (`:27017`), Redis (`:6379`), Kafka (`:9092`), Vault (`:8200`), `ms-config` (`:8888`), `ms-eureka` (`:8761`), `ms-customer` (`:8081`), and `ms-credit` (`:8083`) are running.
2. Run unit tests, Checkstyle validation, and verify the 90% JaCoCo coverage gate:
   ```powershell
   mvn clean verify
   ```
3. Start the microservice locally (port `8082`):
   ```powershell
   mvn spring-boot:run
   ```
4. Build and run as a Docker container (or deploy via `ENVIROMENT/k8s/05-microservices.yaml` in namespace `pivot-banking`):
   ```powershell
   docker build -t pivot/ms-account:1.0.0 .
   docker run -d -p 8082:8082 --name pivot-ms-account pivot/ms-account:1.0.0
   ```
