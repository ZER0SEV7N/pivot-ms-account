# Pivot Banking System - Account Microservice (`pivot-ms-account`)

## Overview
Reactive microservice responsible for managing **Passive Bank Products** (**Savings**, **Checking**, and **Fixed-Term** accounts). It validates customer eligibility rules reactively against `ms-customer` (`:8081`) and active credit card ownership for **VIP/PYME** profiles against `ms-credit` (`:8083`) using **Resilience4j Circuit Breakers (2s timeout)**, enforces minimum opening amounts and free monthly transaction limits with automatic commission calculation, and manages logical lifecycle states (`ACTIVE`, `BLOCKED`, `CLOSED`).

## Architecture, Design Patterns & Security
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) with strict separation of `domain`, `application`, and `infrastructure` layers.
- **Reactive Stack:** Spring Boot 4.1.2 + Spring WebFlux (Netty) + **RxJava 3** (`Single`, `Maybe`, `Flowable`, `Completable`) + Netflix Eureka Client + Resilience4j.
- **SOLID & Design Patterns (Zero God Classes):**
  - **Single Responsibility Principle (SRP):** Pure reactive orchestrators in `application/usecase/` (`CreateAccountUseCaseImpl`, `RetrieveAccountUseCaseImpl`, `FindAccountByIdUseCaseImpl`, `UpdateAccountUseCaseImpl`, `BalanceUseCaseImpl`, `DeleteAccountUseCaseImpl`).
  - **Domain Enricher / Factory Pattern (`AccountDomainInitializer`):** Isolates default metadata initialization, SHA-256 blind-index hashing, minimum opening balance validation, and VIP/PYME product rules from the use case orchestrator.
  - **Domain Policy + Rich Enum Strategy (`AccountBalancePolicy` & `OperationType`):** Encapsulates movement limits, fixed-term calendar windows, automatic commission fees after exceeding `maxFreeMonthlyTransactions`, and polymorphism-based deposit/withdrawal arithmetic without conditional branching (`if/else`).
  - **Strategy Pattern (OCP):** `PersonalAccountStrategy` (enforces single account ownership and active credit card verification for `VIP` Savings accounts) and `BusinessAccountStrategy` (enforces `CHECKING`-only rule, holder requirements, and commission-free maintenance for `PYME` accounts with an active credit card).
  - **Circuit Breaker Pattern (`Resilience4j`):** Protects outbound reactive calls to `ms-customer` and `ms-credit` with a strict **2-second timeout**.
- **Banking-Grade Security:**
  - **AES-256-GCM Encryption at Rest & SHA-256 Blind Indexing** via **HashiCorp Vault** (`secret/pivot-bank`).
  - **Mask-on-Read Presentation:** Full `accountNumber` is returned **only once** upon creation (`POST /accounts`). All subsequent queries mask the number (`************0-01`).
  - **Soft Delete:** `DELETE /accounts/{id}` performs a logical closure (`status = CLOSED`) only when `balance == 0.00`.

## API Endpoints (`/api/v1/accounts`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/accounts` | Open a new bank account (validates VIP/PYME rules & minimum opening balance) |
| `GET` | `/api/v1/accounts` | Retrieve all bank accounts (masked numbers) |
| `GET` | `/api/v1/accounts/{id}` | Retrieve bank account by ID (masked number) |
| `GET` | `/api/v1/accounts/number/{accountNumber}` | Retrieve bank account by number via SHA-256 blind index |
| `GET` | `/api/v1/accounts/customer/{customerId}` | Retrieve all bank accounts belonging to a customer |
| `PUT` | `/api/v1/accounts/{id}` | Update administrative account details (`holders`, `authorizedSigners`, or status) |
| `GET` | `/api/v1/accounts/{id}/balance` | Consult available balance and monthly movements counter |
| `PATCH` | `/api/v1/accounts/{id}/balance` | Process a `DEPOSIT` or `WITHDRAWAL` (calculates automatic commission after free limit) |
| `DELETE` | `/api/v1/accounts/{id}` | Soft delete (`status = CLOSED`) if and only if `balance == 0.00` |

## Inter-Service Communication
- **`pivot-ms-config` (`:8888`):** Fetches externalized configuration and Phase II business rule parameters (`AccountRulesProperties`).
- **`pivot-ms-eureka` (`:8761`):** Registers service instance for dynamic discovery and load balancing via API Gateway (`:8080`).
- **HashiCorp Vault (`:8200`):** Retrieves cryptographic keys (`aes-secret-key`).
- **`pivot-ms-customer` (`:8081`):** Consumed via non-blocking `WebClient` + `Resilience4j` (2s timeout) to verify customer existence, active status, type (`PERSONAL` / `BUSINESS`), and profile (`STANDARD`, `VIP`, `PYME`).
- **`pivot-ms-credit` (`:8083`):** Consumed via non-blocking `WebClient` + `Resilience4j` (2s timeout) to verify active Credit Card ownership when opening `VIP` Savings or `PYME` Checking accounts.
- **`pivot-ms-transaction` (`:8084`):** Consumes `GET /accounts/{id}/balance` and `PATCH /accounts/{id}/balance` to process deposits, withdrawals, and bank transfers.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`).
- **Database Name:** `db_pivot_account`
- **Collection:** `accounts`

## How to Run
1. Ensure MongoDB (`:27017`), HashiCorp Vault (`:8200`), `ms-config` (`:8888`), and `ms-customer` (`:8081`) are running from the `ENVIRONMENT` setup.
2. Create your local `.env` file in the project root (based on `.env.example`) containing your `VAULT_URI` and `VAULT_TOKEN`.
3. Start the microservice:
```powershell
   mvn clean spring-boot:run
```
4. The service will start on port *8082* and register in Eureka as *MS-ACCOUNT*.
5. API Testing: Import *pivot-ms-account.postman_collection.json* into Postman to run all test scenarios.

---