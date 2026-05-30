# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvn clean compile

# Run all tests (uses H2 in-memory — no database required)
mvn test

# Run a single test class
mvn test -Dtest=TransferServiceTest

# Run a single test method
mvn test -Dtest=RetryingTransferServiceTest#allAttemptsFailWithLockException_throwsOptimisticLockingRetryFailed

# Start the application (requires PostgreSQL — see Database Setup below)
mvn spring-boot:run
```

## Database Setup

Production uses PostgreSQL. Set env vars `DB_USER` and `DB_PASSWORD` (defaults: `transfer`/`transfer`), create a `transfer_db` database, then run `src/main/resources/schema.sql` once to create the three tables.

Tests use H2 in-memory with `ddl-auto=create-drop` — no setup required.

## Architecture

The single endpoint `POST /transfers` flows through three layers:

```
TransferController
  → RetryingTransferService   (no @Transactional — owns the retry loop)
    → TransferService         (@Transactional — all business logic in one method)
```

**Why two service layers:** `TransferService.execute(...)` uses optimistic locking (`@Version` on `Account`). When a concurrent write causes `ObjectOptimisticLockingFailureException`, the transaction is broken and cannot be reused. `RetryingTransferService` sits outside any transaction so each retry opens a fresh one. Never add `@Transactional` to `RetryingTransferService`.

**Idempotency:** Every request carries an `Idempotency-Key` header. `TransferService` computes a SHA-256 hash of `{fromAccount, toAccount, amount}` via `RequestHashUtil` and stores it in `idempotency_records`. Same key + same hash → return cached `transferId` with `status: "DUPLICATE"`. Same key + different hash → HTTP 409. A database `UNIQUE` constraint on `idempotency_key` is the race-condition guard when two concurrent first-calls arrive simultaneously.

**`TransferService.execute(...)` step order** (matters — don't reorder):
1. Same-account check → `InvalidTransferException`
2. Hash computation
3. Idempotency lookup (early return or 409)
4. Load source account → `AccountNotFoundException`
5. Load destination account → `AccountNotFoundException`
6. Balance check → `InsufficientFundsException`
7. Mutate balances (Hibernate dirty-checks and flushes on commit)
8. Save `Transfer` record
9. Save `IdempotencyRecord`

**Error handling:** All exceptions are mapped to `{ status, message, timestamp }` JSON in `GlobalExceptionHandler`. HTTP status codes: 400 `InvalidTransferException` / validation, 404 `AccountNotFoundException`, 409 `IdempotencyConflictException` / `DataIntegrityViolationException`, 422 `InsufficientFundsException`, 503 `OptimisticLockingRetryFailedException`.

## Key Constraints

- No Lombok anywhere in this project.
- Always use `BigDecimal.compareTo()`, never `equals()`, for monetary comparisons (`1.00.equals(1.0)` is `false`).
- `transfer.retry.max-attempts` (default `3`) is configurable via `application.properties`.
- `schema.sql` is the source of truth for the database schema — `ddl-auto=validate` in production means the schema must match the JPA mappings exactly.

Architecture Principles

Rich Domain Model

Prefer object-oriented design and encapsulation.

Business rules and state transitions should be owned by the domain objects responsible for that state.

Avoid direct manipulation of entity state from services, controllers, or other external classes.

Prefer behavior-oriented methods over procedural state modifications.

Good examples:

* account.withdraw(amount)
* account.deposit(amount)
* order.cancel()
* subscription.activate()
* transfer.markCompleted()

Avoid patterns like:

* entity.setBalance(...)
* entity.setStatus(...)
* entity.setAmount(...)
* entity.setActive(...)

Responsibility Distribution

Controllers:

* handle HTTP concerns only
* validation and request/response mapping only

Services:

* orchestrate use cases
* coordinate domain objects
* manage transactions
* contain minimal business logic

Entities:

* own their state
* enforce invariants
* contain business behavior
* protect consistency rules

Repositories:

* persistence only
* no business logic

Encapsulation

Prefer asking an object to perform an action rather than modifying its internal state.

When implementing new functionality, first determine which object should be responsible for the behavior and place the behavior there whenever possible.

Code Style Preferences

Prefer intention-revealing methods over direct field manipulation.

Prefer domain methods over chains of getters/setters.

Avoid anemic domain models.

New business rules should generally be implemented inside the domain model rather than in service classes unless there is a strong reason not to.


## Preferred Skills

Before implementing features:
- use feature-planner

When generating backend code:
- use rich-domain-model

Before commit:
- use backend-review

Before PR:
- use security-review