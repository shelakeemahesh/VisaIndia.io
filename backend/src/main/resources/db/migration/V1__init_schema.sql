-- V1: Clean Unified Initial Schema for VisaSetu / VisaIndia.io
-- Strictly aligned with all JPA domain entities

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_TRAVELER',
    full_name VARCHAR(150) DEFAULT 'Traveler',
    password_hash VARCHAR(255),
    nationality VARCHAR(100),
    passport_number VARCHAR(255),
    phone_number VARCHAR(50),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- 2. User Settings Table
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    arrival_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    advisory_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_resync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    language_preference VARCHAR(10) NOT NULL DEFAULT 'en',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Auth OTP Table
CREATE TABLE IF NOT EXISTS auth_otp (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(64) NOT NULL,
    purpose VARCHAR(50) NOT NULL DEFAULT 'LOGIN',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_otp_lookup ON auth_otp(email, purpose, used);
CREATE INDEX idx_auth_otp_email ON auth_otp(email);

-- 4. Visa Rules Table
CREATE TABLE IF NOT EXISTS visa_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nationality VARCHAR(3) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    route_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    fee NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    processing_days_min INT NOT NULL,
    processing_days_max INT NOT NULL,
    validity_days INT NOT NULL,
    max_stay_days INT NOT NULL,
    entries VARCHAR(20) NOT NULL,
    allowed_ports TEXT NOT NULL DEFAULT '[]',
    documents TEXT NOT NULL DEFAULT '[]',
    source_url VARCHAR(500) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    is_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visa_rule_nat_pur ON visa_rule(nationality, purpose);
CREATE INDEX idx_visa_rule_route ON visa_rule(route_type);

-- 5. Trips Table
CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    guest_access_token VARCHAR(128),
    applicant_email VARCHAR(150) NOT NULL,
    applicant_name VARCHAR(255) NOT NULL,
    passport_number_encrypted TEXT,
    date_of_birth_encrypted TEXT,
    nationality VARCHAR(100) NOT NULL,
    category_name VARCHAR(150) NOT NULL DEFAULT 'e-Tourist Visa (30 Days)',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    preparation_status VARCHAR(64) DEFAULT 'PREPARATION_IN_PROGRESS',
    total_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    arrival_date DATE,
    port_of_arrival VARCHAR(100),
    eta_number VARCHAR(100),
    qr_token TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trips_ref ON trips(reference_id);
CREATE INDEX idx_trips_user ON trips(user_id);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_guest_token ON trips(guest_access_token);

-- 6. Trip Modules Table
CREATE TABLE IF NOT EXISTS trip_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    module_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    form_data_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trip_modules_trip ON trip_modules(trip_id);

-- 7. Trip Drafts Table
CREATE TABLE IF NOT EXISTS trip_drafts (
    draft_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    guest_access_token VARCHAR(128),
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

CREATE INDEX idx_trip_drafts_user_id ON trip_drafts(user_id);
CREATE INDEX idx_trip_drafts_email ON trip_drafts(applicant_email);
CREATE INDEX idx_trip_drafts_guest_token ON trip_drafts(guest_access_token);

-- 8. Payment Orders Table
CREATE TABLE IF NOT EXISTS payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    reference_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(100) UNIQUE NOT NULL,
    payment_id VARCHAR(100),
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    idempotency_key VARCHAR(150) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    refund_id VARCHAR(100) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_orders_trip_id ON payment_orders(trip_id);
CREATE INDEX idx_payment_orders_reference_id ON payment_orders(reference_id);
CREATE INDEX idx_payment_orders_order_id ON payment_orders(order_id);
CREATE INDEX idx_payment_orders_idempotency ON payment_orders(idempotency_key);

-- 9. Payment Events Table (Webhook Deduplication)
CREATE TABLE IF NOT EXISTS payment_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    order_id VARCHAR(100),
    payment_id VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 10. Transactional Outbox Table
CREATE TABLE IF NOT EXISTS payment_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) UNIQUE NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'TRIP_PAYMENT',
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_outbox_status ON payment_outbox(status);

-- 11. Visa Category Details Table
CREATE TABLE IF NOT EXISTS visa_category_detail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    sub_category_name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    purpose_code VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    who_is_it_for TEXT NOT NULL,
    validity_text VARCHAR(255) NOT NULL,
    validity_days INT NOT NULL,
    entries VARCHAR(50) NOT NULL,
    max_stay_text VARCHAR(255) NOT NULL,
    max_stay_days INT NOT NULL,
    fee_band_summary VARCHAR(255) NOT NULL,
    processing_days_min INT NOT NULL,
    processing_days_max INT NOT NULL,
    is_extendable BOOLEAN NOT NULL DEFAULT FALSE,
    is_convertible BOOLEAN NOT NULL DEFAULT FALSE,
    required_documents TEXT NOT NULL DEFAULT '[]',
    entry_point_limits TEXT,
    official_source_url VARCHAR(500) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visa_cat_slug ON visa_category_detail(slug);
CREATE INDEX idx_visa_cat_purpose ON visa_category_detail(purpose_code);

-- 12. Entry Points Table
CREATE TABLE IF NOT EXISTS entry_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(20),
    type VARCHAR(50) NOT NULL,
    state VARCHAR(100) NOT NULL,
    is_evisa_entry BOOLEAN NOT NULL DEFAULT TRUE,
    is_evisa_exit BOOLEAN NOT NULL DEFAULT TRUE,
    is_voa_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    official_source_url VARCHAR(500) NOT NULL
);

CREATE INDEX idx_entry_points_type ON entry_points(type);
CREATE INDEX idx_entry_points_state ON entry_points(state);

-- 13. Notices Table
CREATE TABLE IF NOT EXISTS notices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notice_id VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    source_url VARCHAR(500) NOT NULL,
    is_pinned_home BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_notices_pub ON notices(published_at DESC);
CREATE INDEX idx_notices_pinned ON notices(is_pinned_home);

-- 14. FAQs Table
CREATE TABLE IF NOT EXISTS faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    order_num INT NOT NULL DEFAULT 0,
    official_source_url VARCHAR(500) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_faqs_topic ON faqs(topic);

-- 15. Content Draft Table
CREATE TABLE IF NOT EXISTS content_draft (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key VARCHAR(100) NOT NULL,
    category_slug VARCHAR(100),
    source_url VARCHAR(500) NOT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'OFFICIAL',
    content_hash VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content_json TEXT NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_draft_section ON content_draft(section_key);

-- 16. Demo Applications Table (widened nationality & category)
CREATE TABLE IF NOT EXISTS demo_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(50) UNIQUE NOT NULL,
    applicant_email VARCHAR(255) NOT NULL,
    applicant_name VARCHAR(255) NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    category_name VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL,
    order_id VARCHAR(100),
    payment_id VARCHAR(100),
    amount NUMERIC(10,2) NOT NULL,
    arrival_date DATE,
    port_of_arrival VARCHAR(100),
    qr_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_demo_apps_ref ON demo_applications(reference_id);
CREATE INDEX idx_demo_apps_email ON demo_applications(applicant_email);

-- 17. Help Tickets Table
CREATE TABLE IF NOT EXISTS help_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id VARCHAR(64) UNIQUE NOT NULL,
    category VARCHAR(64) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    email VARCHAR(255) NOT NULL,
    reference_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_help_tickets_email ON help_tickets(email);
CREATE INDEX idx_help_tickets_ticket_id ON help_tickets(ticket_id);
