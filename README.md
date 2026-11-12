# Pivot Banking System - Account Microservice (`pivot-ms-account`)

## Overview
Reactive microservice responsible for managing **Passive Bank Products** (**Savings**, **Checking**, and **Fixed-Term** accounts). It validates customer eligibility rules reactively against `ms-customer`, enforces monthly movement limits and maintenance fees, processes account balance operations, and manages logical lifecycle states (`ACTIVE`, `BLOCKED`, `CLOSED`).

## Architecture, Design Patterns & Security
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) with strict separation of `domain`, `application`, and `infrastructure` layers.
- **Reactive Stack:** Spring Boot 4 + Spring WebFlux (Netty) + **RxJava 3** (`Single`, `Maybe`, `Flowable`, `Completable`).
- **SOLID & Design Patterns:**
  - **Single Responsibility Principle (SRP):** Dedicated Use Case implementations (`CreateAccountUseCaseImpl`, `RetrieveAccountUseCaseImpl`, `FindAccountByIdUseCaseImpl`, `UpdateAccountUseCaseImpl`, `BalanceUseCaseImpl`, `DeleteAccountUseCaseImpl`), plus strict separation between cryptographic operations (`CryptoUtil`) and presentation masking (`DataMaskingUtil`).
  - **Strategy Pattern (OCP):** `PersonalAccountStrategy` (max 1 Savings or 1 Checking account, multiple Fixed-Term accounts, no maintenance fee on Savings/Fixed-Term) and `BusinessAccountStrategy` (multiple Checking accounts allowed with holders/signers, no Savings or Fixed-Term accounts allowed).
  - **Immutable DTOs:** Java 17 `record` types across all web and client payloads.
- **Banking-Grade Security:**
  - **AES-256-GCM Encryption at Rest:** Account numbers are encrypted with a 12-byte random IV and 128-bit authentication tag before persisting in MongoDB.
  - **SHA-256 Blind Indexing:** Exact account number lookups (`GET /accounts/number/{accountNumber}`) use an irreversible SHA-256 hash (`accountNumberHash`) without exposing plain text or leaking the hash in API responses.
  - **Mask-on-Read Presentation:** Full `accountNumber` is returned **only once** upon account creation (`POST /accounts`). All subsequent queries (`GET`, `PUT`, `PATCH`) and console logs mask the number (`************0-01`).
  - **HashiCorp Vault Integration:** Zero hardcoded secrets in source code; `aes-secret-key` is injected at runtime from Vault (`secret/pivot-bank`).
  - **Soft Delete & State Machine:** `DELETE /accounts/{id}` performs a logical closure (`status = CLOSED`) only when `balance == 0.00`.

## API Endpoints (`/api/v1/accounts`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/accounts` | Open a new bank account (returns unmasked account number once) |
| `GET` | `/api/v1/accounts` | Retrieve all bank accounts (masked numbers) |
| `GET` | `/api/v1/accounts/{id}` | Retrieve bank account by ID (masked number) |
| `GET` | `/api/v1/accounts/number/{accountNumber}` | Retrieve bank account by number via SHA-256 blind index |
| `GET` | `/api/v1/accounts/customer/{customerId}` | Retrieve all bank accounts belonging to a customer |
| `PUT` | `/api/v1/accounts/{id}` | Update administrative account details (`holders`, `authorizedSigners`, or `BLOCKED`/`ACTIVE` status) |
| `GET` | `/api/v1/accounts/{id}/balance` | Consult available balance and monthly movements counter |
| `PATCH` | `/api/v1/accounts/{id}/balance` | Process a `DEPOSIT` or `WITHDRAWAL` (consumed by `ms-transaction`) |
| `DELETE` | `/api/v1/accounts/{id}` | Soft delete (`status = CLOSED`) if and only if `balance == 0.00` |

## Inter-Service Communication
- **`pivot-ms-config` (`:8888`):** Fetches externalized configuration and business rule parameters (`AccountRulesProperties`).
- **HashiCorp Vault (`:8200`):** Retrieves cryptographic keys (`aes-secret-key`).
- **`pivot-ms-customer` (`:8081`):** Consumed via non-blocking `WebClient` to verify customer existence, active status, and type (`PERSONAL` vs. `BUSINESS`).
- **`pivot-ms-transaction` (`:8084`):** Consumes `GET /accounts/{id}/balance` and `PATCH /accounts/{id}/balance` to process deposits and withdrawals.

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
4. The service will start on port *8082*.
5. API Testing: Import *pivot-ms-account.postman_collection.json* into Postman to run all test scenarios.

---