-- =========================================================================
-- BLE Booking System - Initial Database Schema (Task T-011)
-- =========================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS btree_gist; -- Needed for exclusion constraints with timestamps

-- 1. USER
CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255), -- nullable
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    start_semester VARCHAR(20),
    field_of_study VARCHAR(255), 
    failed_attempts BIGINT DEFAULT 0,
    locked_until TIMESTAMPTZ
);

-- 2. RESOURCE
CREATE TABLE resource (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price_per_hour DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    type VARCHAR(100) NOT NULL,
    position VARCHAR(255)
);

-- 3. TERMINAL (1:1 with Resource via UNIQUE constraint)
CREATE TABLE terminal (
    id BIGSERIAL PRIMARY KEY,
    public_key VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resource_id BIGINT UNIQUE NOT NULL REFERENCES resource(id) ON DELETE CASCADE
);

-- 4. BOOKING (Including Exclusion Constraint against overlaps & end > start check)
CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resource(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    locked_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_booking_time CHECK (end_time > start_time),
    -- Prevents overlapping active bookings for the same resource
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    )
);

-- 5. DEVICE
CREATE TABLE device (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    public_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

-- 6. REFRESH_TOKEN
CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

-- 7. ONE_TIME_TOKEN (formerly invitation)
CREATE TABLE one_time_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_by_user_id BIGINT REFERENCES "user"(id) ON DELETE SET NULL
);

-- 8. RECEIPT (formerly access_event)
CREATE TABLE receipt (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id BIGINT NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    terminal_id BIGINT NOT NULL REFERENCES terminal(id) ON DELETE CASCADE,
    ticket_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    receipt_time TIMESTAMPTZ NOT NULL,
    signature VARCHAR(255) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. INVOICE (1:1 with Booking via UNIQUE constraint)
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    booking_id BIGINT UNIQUE NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    final_price DECIMAL(10,2) NOT NULL,
    pdf_file_path VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    type VARCHAR(100) NOT NULL,
    qr_reference VARCHAR(255),
    due_date DATE NOT NULL,
    paid_at TIMESTAMPTZ,
    currency VARCHAR(10) NOT NULL DEFAULT 'CHF'
);

-- 10. AVAILABILITY
CREATE TABLE availability (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resource(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    permitted_role VARCHAR(20) NOT NULL
);

-- 11. AUDIT_LOG
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES "user"(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    target_entity VARCHAR(20) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    details JSONB,
    target_id BIGINT,
    outcome VARCHAR(20) NOT NULL,
    ip_address VARCHAR(39)
);