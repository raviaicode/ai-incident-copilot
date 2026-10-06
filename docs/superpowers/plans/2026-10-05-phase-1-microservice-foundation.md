# Phase 1: Tenant-aware microservice foundation

This plan is reconstructed from the Phase 1 scope and acceptance criteria in the project README.

## Delivery slices

1. Create a Java 21 Maven reactor and shared immutable tenant/event contracts.
2. Add tenant validation and an order HTTP boundary that rejects missing or malformed context.
3. Persist tenant-scoped orders and publish `OrderCreated` events.
4. Consume orders idempotently, persist payments, and publish `PaymentProcessed` events.
5. Consume payments idempotently and persist delivered notifications.
6. Package PostgreSQL, Redis, Kafka, and all services behind `docker compose up --build`.
7. Run unit tests for validation, unknown tenants, context propagation, and duplicate delivery.

## Definition of done

- `mvn test` passes without manually running infrastructure.
- `docker compose up --build` starts the complete Phase 1 stack.
- A valid `POST /orders` returns `202`; missing context returns `400`; an unknown tenant returns `422`.
- Tenant-scoped repository methods prevent unqualified cross-tenant reads.
- Database uniqueness plus consumer checks make payment and notification delivery idempotent.
