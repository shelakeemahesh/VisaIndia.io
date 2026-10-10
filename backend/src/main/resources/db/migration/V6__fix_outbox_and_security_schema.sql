-- V6: Fix outbox alignment, payment events deduplication, trip drafts ownership, and security constraints

-- 1. Payment Outbox Alignment with JPA Entity
-- Ensure payload column and event_id exist
ALTER TABLE payment_outbox ADD COLUMN IF NOT EXISTS event_id VARCHAR(100);
ALTER TABLE payment_outbox ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE payment_outbox ADD COLUMN IF NOT EXISTS published_at TIMESTAMP WITH TIME ZONE;

-- 2. Durable Webhook Events Deduplication Table
CREATE TABLE IF NOT EXISTS payment_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    order_id VARCHAR(100),
    payment_id VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Payment Orders Table
CREATE TABLE IF NOT EXISTS payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    reference_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(100) UNIQUE NOT NULL,
    payment_id VARCHAR(100),
    signature VARCHAR(255),
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    idempotency_key VARCHAR(150) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_orders_trip_id ON payment_orders(trip_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_reference_id ON payment_orders(reference_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_order_id ON payment_orders(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_idempotency ON payment_orders(idempotency_key);

-- 4. Trip Drafts Table with Explicit User Ownership
CREATE TABLE IF NOT EXISTS trip_drafts (
    draft_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    applicant_email VARCHAR(150) NOT NULL,
    current_step INT NOT NULL DEFAULT 1,
    nationality VARCHAR(100),
    category_name VARCHAR(150),
    shared_profile_json TEXT,
    modules_json TEXT,
    is_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    trip_id UUID REFERENCES trips(id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trip_drafts_user_id ON trip_drafts(user_id);
CREATE INDEX IF NOT EXISTS idx_trip_drafts_email ON trip_drafts(applicant_email);

-- 5. Extend Trips with Missing Columns
ALTER TABLE trips ADD COLUMN IF NOT EXISTS total_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS category_name VARCHAR(150) DEFAULT 'e-Tourist Visa (30 Days)';
ALTER TABLE trips ADD COLUMN IF NOT EXISTS eta_number VARCHAR(100);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS qr_token TEXT;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITH TIME ZONE;

-- 6. Auth OTP Table Updates for SHA-256 and Attempt Limits
ALTER TABLE auth_otp ALTER COLUMN otp_code TYPE VARCHAR(64);
ALTER TABLE auth_otp ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0;

-- 7. Demo Applications Table (for isolated test/dev environments)
CREATE TABLE IF NOT EXISTS demo_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(50) UNIQUE NOT NULL,
    applicant_email VARCHAR(150) NOT NULL,
    applicant_name VARCHAR(150) NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    category_name VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNDER_REVIEW',
    order_id VARCHAR(100),
    payment_id VARCHAR(100),
    amount NUMERIC(10,2) NOT NULL DEFAULT 25.00,
    arrival_date DATE,
    port_of_arrival VARCHAR(150),
    qr_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_demo_apps_ref ON demo_applications(reference_id);
CREATE INDEX IF NOT EXISTS idx_demo_apps_email ON demo_applications(applicant_email);
