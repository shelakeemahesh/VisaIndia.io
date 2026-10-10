# ADR-003: Idempotent Payment Webhooks & Automated Reconciliation

## Status
Accepted

## Context
Traveler payment processing is susceptible to two catastrophic failures:
1. Traveler card is debited, but browser closes or network drops before returning to the merchant site, leaving the application in `PAYMENT_PENDING`.
2. Webhooks may arrive multiple times (at-least-once delivery by Razorpay), risking double processing or state corruptions.

## Decision
1. **Server-Computed Amount**:
   - Order creation amount is strictly calculated on the backend from the DB `visa_rule.fee`, never trusted from the client.
2. **Idempotency Keys**:
   - Order creation accepts an `Idempotency-Key` cached in Redis. Re-submitting the same key returns the existing order.
3. **Webhook as Source of Truth**:
   - Razorpay HMAC SHA-256 signature is verified before processing.
   - Incoming webhook events are deduplicated by `event_id` in Redis/PostgreSQL.
   - The browser redirect handler shares the exact same idempotent payment service.
4. **Automated Reconciliation**:
   - A background job (`@Scheduled`) queries Razorpay API every 5 minutes for orders remaining in `PAYMENT_PENDING` for more than 10 minutes.
   - If the payment was captured on Razorpay, state self-heals to `PAID`.
   - If orphan/duplicate payments are detected, auto-refund is initiated.
   - Unresolved discrepancies are flagged in the admin portal for operator review.

## Consequences
- **Positive**: Guaranteed zero orphan debits, idempotent processing, self-healing payment state.
- **Trade-off**: Requires periodic background polling against Razorpay API.
