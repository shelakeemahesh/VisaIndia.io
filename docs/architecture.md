# VisaSetu System Architecture

## Architecture Diagram

```mermaid
flowchart TB
    subgraph Client["Traveler & Admin Client (React 18 + Vite + TS)"]
        UI["Modern Responsive UI<br/>(Tailwind CSS + Lucide)"]
        IDB["IndexedDB Mirror<br/>(Offline Draft Storage)"]
        I18N["react-i18next<br/>(12 Languages + RTL)"]
    end

    subgraph Gateway["Edge & Security Layer"]
        CORS["CORS & Rate Limiter"]
        CORR["Correlation ID Interceptor<br/>(X-Correlation-ID)"]
        JWT["Spring Security JWT Filter"]
    end

    subgraph Backend["Spring Boot 3.3 Backend (Java 21)"]
        ELIG["Eligibility Engine<br/>(/api/v1/eligibility)"]
        WIZARD["Trip & Profile Service<br/>(/api/v1/trips)"]
        PAY["Razorpay Payment Service<br/>(/api/v1/payments)"]
        WEBHOOK["Signature-Verified Webhook<br/>(/api/v1/webhooks/razorpay)"]
        RECON["Scheduled Reconciliation Job<br/>(Stuck Payments > 10m)"]
        OUTBOX_PUB["Transactional Outbox Publisher<br/>(Polling / CDC)"]
        GOV_MOCK["Mock Government Decision Engine<br/>(Auto-Review & Delay)"]
    end

    subgraph Data["Persistence & Storage Layer"]
        PG[("PostgreSQL 16 (System of Record)<br/>- visa_rule<br/>- users<br/>- trips (snapshots)<br/>- payments<br/>- payment_outbox")]
        MONGO[("MongoDB 7 (Dynamic Documents)<br/>- form_modules (Schemas)<br/>- draft_applications (Autosaved)<br/>- audit_logs")]
        REDIS[("Redis 7 (Hot Cache & State)<br/>- Idempotency Keys<br/>- Rate Limits<br/>- Email OTPs")]
        MINIO[("MinIO (S3 Object Storage)<br/>- Passport Scans<br/>- Photos<br/>- Generated PDF Grants")]
        RABBIT[("RabbitMQ 3.13<br/>- payment.events.exchange<br/>- status.update.queue<br/>- DLQ")]
        MAIL[("MailHog (Dev SMTP)<br/>- Magic Links & OTPs<br/>- Decision Notifications")]
    end

    UI <--> IDB
    UI --> CORR --> CORS --> JWT
    JWT --> ELIG
    JWT --> WIZARD
    JWT --> PAY
    WEBHOOK --> PAY

    ELIG --> PG
    WIZARD --> MONGO
    WIZARD --> PG
    WIZARD --> MINIO
    PAY --> PG
    PAY --> REDIS
    PAY --> OUTBOX_PUB
    OUTBOX_PUB --> RABBIT
    RABBIT --> GOV_MOCK
    GOV_MOCK --> PG
    GOV_MOCK --> MAIL
    RECON --> PG
    RECON --> PAY
```

## Architectural Tenets
1. **Separation of System of Record & Working Drafts**:
   - PostgreSQL holds submitted, immutable financial, trip, and rule data.
   - MongoDB holds dynamic JSON schema-driven module forms and fast autosaves.
2. **Never Lose Data**:
   - IndexedDB in the browser acts as a local sync buffer.
   - Drafts are continuously debounced (1.5s) to MongoDB with versioned optimistic locking.
3. **Financial State Correctness**:
   - Webhook is the single source of truth for payment success.
   - Idempotency keys stored in Redis prevent duplicate charges.
   - Transactional outbox ensures eventual consistency between payment ledger and notification/review queues.
   - Automated reconciliation self-heals stranded pending payments.
