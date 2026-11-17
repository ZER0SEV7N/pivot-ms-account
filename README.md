# Pivot Banking System - Account Microservice (`pivot-ms-account`)

## Overview
Reactive microservice responsible for managing **Passive Bank Products** (**Savings**, **Checking**, and **Fixed-Term** accounts). In Phase II, it validates customer profiles (`STANDARD`, `VIP`, `PYME`) against `ms-customer` (`:8081`) and verifies active Credit Card ownership against `ms-credit` (`:8083`) using **Resilience4j Circuit Breakers (2-second timeout)**, enforces minimum opening balances (`minOpeningAmount`), and automatically calculates transaction commission fees (`lastAppliedCommission`) once free monthly movements (`maxFreeMonthlyTransactions`) are exceeded.

## Architecture, Design Patterns & Quality Gates
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) with strict separation of `domain`, `application`, and `infrastructure` layers.
- **Reactive Stack:** Java 17 + Spring Boot 4.1.2 + Spring WebFlux (Netty) + **RxJava 3** (`Single`, `Maybe`, `Flowable`, `Completable`) + Netflix Eureka Client + **Resilience4j**.
- **SOLID & Design Patterns (Zero God Classes):**
  - **Single Responsibility Principle (SRP):** Pure reactive orchestrators in `application/usecase/` (`CreateAccountUseCaseImpl`, `RetrieveAccountUseCaseImpl`, `FindAccountByIdUseCaseImpl`, `UpdateAccountUseCaseImpl`, `BalanceUseCaseImpl`, `DeleteAccountUseCaseImpl`).
  - **Domain Enricher / Factory Pattern (`AccountDomainInitializer`):** Located in `application/support/initializers/`, isolating metadata initialization, SHA-256 blind indexing, minimum opening balance validation, and `VIP`/`PYME` product defaults.
  - **Domain Policy + Rich Enum Strategy (`AccountBalancePolicy` & `OperationType`):** Encapsulates movement limits, fixed-term calendar windows, automatic commission fees after exceeding `maxFreeMonthlyTransactions`, and polymorphic deposit/withdrawal arithmetic without conditional branching.
  - **Strategy Pattern (OCP):** `PersonalAccountStrategy` (enforces single account ownership and active credit card requirement for `VIP` Savings accounts) and `BusinessAccountStrategy` (enforces `CHECKING`-only rule, holder requirements, and `maintenanceFee = 0.00` for `PYME` accounts with an active credit card).
  - **Circuit Breaker Pattern (`Resilience4j`):** Protects outbound reactive calls in `CustomerWebClientAdapter` and `CreditWebClientAdapter` with a strict **2-second timeout**.
- **Code Quality & Testing:**
  - **Google Java Style (`maven-checkstyle-plugin`):** Enforced during Maven `validate` phase.
  - **Unit Testing (`JUnit 5` + `Mockito` + `RxJava 3 TestObserver` + `StepVerifier`):** AAA pattern and FIRST principles achieving **>90% code coverage** in **JaCoCo** and **SonarQube**.

## Security & Secrets (`HashiCorp Vault`)
- **Vault Path:** `secret/pivot-bank`
- **Required Secret Key:** `aes-secret-key` (AES-256-GCM encryption at rest and SHA-256 blind indexing for `accountNumber`).
- **Mask-on-Read Presentation:** Full `accountNumber` is returned **only once** upon creation (`POST /accounts`). All subsequent reads mask the number (`************0-01`).
- **Soft Delete:** `DELETE /accounts/{id}` performs a logical closure (`status = CLOSED`) only when `balance == 0.00`.

## API Endpoints (`/api/v1/accounts`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/accounts` | Open a new bank account (validates `VIP`/`PYME` active credit card & `minOpeningAmount`) |
| `GET` | `/api/v1/accounts` | Retrieve all bank accounts (masked numbers) |
| `GET` | `/api/v1/accounts/{id}` | Retrieve bank account by ID |
| `GET` | `/api/v1/accounts/number/{accountNumber}` | Retrieve bank account by number via SHA-256 blind index |
| `GET` | `/api/v1/accounts/customer/{customerId}` | Retrieve all bank accounts belonging to a customer |
| `PUT` | `/api/v1/accounts/{id}` | Update account holders, authorized signers, or status |
| `GET` | `/api/v1/accounts/{id}/balance` | Consult available balance and monthly movements counter |
| `PATCH` | `/api/v1/accounts/{id}/balance` | Process `DEPOSIT` or `WITHDRAWAL` (applies automatic commission after free limit) |
| `DELETE` | `/api/v1/accounts/{id}` | Soft delete (`status = CLOSED`) if `balance == 0.00` |

## Inter-Service Communication & Observability
- **`pivot-ms-config` (`:8888`):** Fetches externalized rules (`AccountRulesProperties`).
- **`pivot-ms-eureka` (`:8761`):** Registers service instance as `MS-ACCOUNT`.
- **`pivot-ms-customer` (`:8081`):** Consumed via `WebClient` + `Resilience4j` (2s timeout) to validate customer status, type, and profile (`VIP`/`PYME`).
- **`pivot-ms-credit` (`:8083`):** Consumed via `WebClient` + `Resilience4j` (2s timeout) to verify active credit card ownership for `VIP` and `PYME` accounts.
- **Consumed by:** `pivot-ms-transaction` (`:8084`) for deposits, withdrawals, and inter-account transfers.
- **Observability:** Structured logs with `X-Correlation-Id` exported to **Promtail -> Loki -> Grafana (`:3000`)**.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7.0 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`).
- **Database Name:** `db_pivot_account`
- **Collection:** `accounts`

## How to Run (Local & Docker)
1. Ensure MongoDB (`:27017`), Vault (`:8200`), `ms-config` (`:8888`), `ms-eureka` (`:8761`), `ms-customer` (`:8081`), and `ms-credit` (`:8083`) are running.
2. Run unit tests, Checkstyle, and generate the JaCoCo coverage report:
   ```powershell
   mvn clean test checkstyle:check jacoco:report
   ```
3. Start the microservice locally:
   ```powershell
   mvn spring-boot:run
   ```
4. Build and run as an independent Docker container:
   ```powershell
   docker build -t pivot-ms-account:2.0.0 .
   docker run -d -p 8082:8082 --name pivot-ms-account pivot-ms-account:2.0.0
   ```