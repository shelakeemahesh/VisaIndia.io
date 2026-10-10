# ADR-001: Transactional Outbox Pattern for Payment & State Events

## Status
Accepted

## Context
When a payment succeeds or an application state transitions, downstream actions must occur reliably (e.g., triggering government mock evaluation, sending applicant emails, notifying analytics).
Directly publishing to RabbitMQ inside an ACID database transaction introduces dual-write hazards:
1. The message may be sent, but the database transaction rolls back, producing phantom events.
2. The database transaction commits, but the message broker is unreachable, dropping critical events.

## Decision
We implement the **Transactional Outbox Pattern**:
1. Every payment or status update writes an event record to the `payment_outbox` table in PostgreSQL within the exact same database transaction as the business entity update.
2. A resilient background publisher (or CDC process) reads pending rows from `payment_outbox`, dispatches them to RabbitMQ with exponential retry, and marks them `PUBLISHED` upon broker acknowledgment.
3. Consumers subscribe with idempotent deduplication based on `event_id`.
4. A Dead Letter Queue (DLQ) captures failed deliveries after maximum retry attempts.

## Consequences
- **Positive**: Guaranteed at-least-once event delivery; zero risk of distributed dual-write inconsistency.
- **Trade-off**: Requires scheduled polling or CDC and message deduplication on consumers.
