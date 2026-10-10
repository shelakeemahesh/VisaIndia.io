# ADR-002: Dual-Store Strategy (PostgreSQL System of Record & MongoDB Dynamic Forms)

## Status
Accepted

## Context
VisaSetu supports multiple dynamic modules:
- Standard e-Visa (30 days, 1 yr, 5 yr, business, medical)
- e-Arrival Card (Air Suvidha customs & flight details)
- e-Suvidha (Health declaration and vaccination records)

Different nationalities and routes require different fields and validations that evolve dynamically over time. Storing active work-in-progress drafts across multiple screens requires frequent debounced autosaves without strict schema constraints. Conversely, submitted applications, payment records, and visa rules require strict ACID guarantees, foreign keys, and relational auditing.

## Decision
We adopt a **Dual-Store Strategy**:
1. **MongoDB**:
   - Stores dynamic `FormModule` schema definitions (JSON Schema + field mappings).
   - Stores autosaved draft applications with optimistic locking (`version` field) and quick document writes.
   - Stores audit logs.
2. **PostgreSQL**:
   - Serves as the authoritative System of Record (SoR).
   - Stores official `visa_rule`, `users`, validated submitted `trips` (with encrypted PII), `payments`, and `payment_outbox`.
   - On submission, an atomic snapshot is taken from the validated MongoDB draft into PostgreSQL.

## Consequences
- **Positive**: High schema flexibility and autosave performance in MongoDB; rigorous consistency, relational integrity, and encryption in PostgreSQL.
- **Trade-off**: Requires synchronizing the transition between draft mode and submitted application.
