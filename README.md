# Pivot Banking System - Account Microservice (`pivot-ms-account`)

## Overview
Reactive microservice responsible for managing **Passive Bank Products** (**Savings**, **Checking**, and **Fixed-Term** accounts). It validates customer eligibility rules reactively against `ms-customer`, enforces monthly movement limits, manages account balances, and handles logical lifecycle states (`ACTIVE`, `BLOCKED`, `CLOSED`).

## Architecture & Security Highlights
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) with strict separation of `domain`, `application`, and `infrastructure` layers.
- **Reactive Stack:** Spring WebFlux (Netty) + **RxJava 3** (`Single`, `Maybe`, `Flowable`, `Completable`).
- **Design Patterns & SOLID:**
  - **Strategy Pattern:** `PersonalAccountStrategy` and `BusinessAccountStrategy` for product ownership rules.
  - **Single Responsibility Principle (SRP):** Separation of administrative account management (`AccountUseCaseImpl`) from transactional balance operations (`BalanceUseCaseImpl`), and cryptographic operations (`CryptoUtil`) from log masking (`DataMaskingUtil`).
  - **Immutable DTOs:** Java 17 `record` types across all web and client payloads.
- **Banking-Grade Security:**
  - **AES-256-GCM Encryption at Rest:** Account numbers are encrypted with a 12-byte random IV and 128-bit authentication tag before persisting in MongoDB.
  - **SHA-256 Blind Indexing:** Exact account number lookups use an irreversible SHA-256 hash (`accountNumberHash`) without exposing plain text.
  - **HashiCorp Vault Integration:** Zero hardcoded secrets in source code; `aes-secret-key` is injected at runtime from Vault (`secret/pivot-bank`).
  - **Soft Delete & Unblocking:** Account deletion (`DELETE`) performs a logical closure (`status = CLOSED`) only when `balance == 0.00`. Blocked accounts can be restored via `PATCH /{id}/reactivate`.

## Inter-Service Communication
- **`ms-config` (`:8888`):** Fetches externalized configuration and business rule parameters (`AccountRulesProperties`).
- **HashiCorp Vault (`:8200`):** Retrieves cryptographic keys (`aes-secret-key`).
- **`ms-customer` (`:8081`):** Consumed via non-blocking `WebClient` to verify customer existence, status, and type (`PERSONAL` vs. `BUSINESS`).
- **`ms-transaction` (`:8084`):** Consumes `GET /accounts/{id}/balance` and `PATCH /accounts/{id}/balance` to process deposits and withdrawals.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`).
- **Database Name:** `db_pivot_account`
- **Collection:** `accounts`

## How to Run
1. Start the MongoDB container:
   ```bash
   docker compose up -d

    ```

2. Start your local **HashiCorp Vault** instance (`:8200`) and load the `secret/pivot-bank` secrets (`aes-secret-key`).
3. Ensure **`ms-config`** (`:8888`) and **`ms-customer`** (`:8081`) are running.
4. Export your Vault token environment variable (see `.env.example`):
    ```powershell
    $env:VAULT_TOKEN="your_local_vault_token"
    mvn clean spring-boot:run
    ```
5. The service will start on port **`8082`**.
6. **API Testing:** Import **`pivot-ms-account.postman_collection.json`** into Postman to run all test scenarios.
