-- V5: User Auth, Profile, and Settings Schema for Dashboard

-- 1. Extend Users table with profile and security columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(150) DEFAULT 'Traveler';
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS nationality VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS passport_number VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. User Settings table for notification services & dashboard preferences
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    arrival_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    advisory_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_resync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    language_preference VARCHAR(10) NOT NULL DEFAULT 'en',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Auth OTP table for password reset and OTP verification tracking
CREATE TABLE IF NOT EXISTS auth_otp (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL DEFAULT 'LOGIN',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auth_otp_email ON auth_otp(email);

-- 4. Seed demo users and settings
INSERT INTO users (id, email, full_name, nationality, passport_number, role, is_active, email_verified, created_at)
VALUES 
  ('11111111-1111-1111-1111-111111111111', 'kenji.sato@tokyo-travel.jp', 'Kenji Sato', 'Japan', 'TK-8849201', 'ROLE_TRAVELER', true, true, NOW()),
  ('22222222-2222-2222-2222-222222222222', 'e.watson@london-tech.co.uk', 'Emma Watson', 'United Kingdom', 'GB-7491028', 'ROLE_TRAVELER', true, true, NOW()),
  ('33333333-3333-3333-3333-333333333333', 'max.mueller@berlin-health.de', 'Max Müller', 'Germany', 'DE-9102847', 'ROLE_TRAVELER', true, true, NOW())
ON CONFLICT (email) DO UPDATE SET 
  full_name = EXCLUDED.full_name,
  nationality = EXCLUDED.nationality,
  passport_number = EXCLUDED.passport_number;

INSERT INTO user_settings (user_id, email_notifications_enabled, arrival_reminders_enabled, advisory_alerts_enabled, auto_resync_enabled, language_preference)
SELECT id, true, true, true, true, 'en' FROM users
ON CONFLICT (user_id) DO NOTHING;
