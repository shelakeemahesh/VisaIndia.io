-- V3: Schema for Content Ingestion Pipeline, Notices, Entry Points, FAQs & Demo Applications

CREATE TABLE IF NOT EXISTS content_draft (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key VARCHAR(100) NOT NULL,
    category_slug VARCHAR(100),
    source_url VARCHAR(500) NOT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'OFFICIAL',
    content_hash VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content_json JSONB NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_draft_section ON content_draft(section_key);

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
    required_documents JSONB NOT NULL DEFAULT '[]'::jsonb,
    entry_point_limits TEXT,
    official_source_url VARCHAR(500) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visa_cat_slug ON visa_category_detail(slug);
CREATE INDEX idx_visa_cat_purpose ON visa_category_detail(purpose_code);

CREATE TABLE IF NOT EXISTS entry_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(20),
    type VARCHAR(50) NOT NULL, -- AIRPORT, SEAPORT, LAND_ICP
    state VARCHAR(100) NOT NULL,
    is_evisa_entry BOOLEAN NOT NULL DEFAULT TRUE,
    is_evisa_exit BOOLEAN NOT NULL DEFAULT TRUE,
    is_voa_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    official_source_url VARCHAR(500) NOT NULL
);

CREATE INDEX idx_entry_points_type ON entry_points(type);
CREATE INDEX idx_entry_points_state ON entry_points(state);

CREATE TABLE IF NOT EXISTS notices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notice_id VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(50) NOT NULL, -- POLICY, ENTRY_POINTS, FEES, MAINTENANCE, SYSTEM
    severity VARCHAR(50) NOT NULL DEFAULT 'NORMAL', -- CRITICAL, HIGH, NORMAL, LOW
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    source_url VARCHAR(500) NOT NULL,
    is_pinned_home BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_notices_pub ON notices(published_at DESC);
CREATE INDEX idx_notices_pinned ON notices(is_pinned_home);

CREATE TABLE IF NOT EXISTS faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(50) NOT NULL, -- BEFORE_APPLYING, APPLICATION, PAYMENT, AFTER_APPROVAL, ARRIVAL, TECHNICAL
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    order_num INT NOT NULL DEFAULT 0,
    official_source_url VARCHAR(500) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_faqs_topic ON faqs(topic);

CREATE TABLE IF NOT EXISTS demo_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(50) UNIQUE NOT NULL,
    applicant_email VARCHAR(255) NOT NULL,
    applicant_name VARCHAR(255) NOT NULL,
    nationality VARCHAR(3) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
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
