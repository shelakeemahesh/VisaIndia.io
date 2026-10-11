-- V8: Align schema with entity additions, guest access tokens, and password revocation timestamp

-- 1. Guest access tokens & preparation status for trips
ALTER TABLE trips ADD COLUMN IF NOT EXISTS guest_access_token VARCHAR(128);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS preparation_status VARCHAR(64) DEFAULT 'PREPARATION_IN_PROGRESS';
CREATE INDEX IF NOT EXISTS idx_trips_guest_token ON trips(guest_access_token);

-- 2. Guest access tokens for trip drafts
ALTER TABLE trip_drafts ADD COLUMN IF NOT EXISTS guest_access_token VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_trip_drafts_guest_token ON trip_drafts(guest_access_token);

-- 3. Password changed timestamp on users table for session revocation
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP WITH TIME ZONE;

-- 4. Auth OTP table performance index
CREATE INDEX IF NOT EXISTS idx_auth_otp_lookup ON auth_otp(email, purpose, used, expires_at);
