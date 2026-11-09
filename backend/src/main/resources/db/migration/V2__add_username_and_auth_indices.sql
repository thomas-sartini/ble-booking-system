-- =========================================================================
-- BLE Booking System - Add Username and Authentication Indices (Task T-030)
-- =========================================================================
-- Adds username column for FR-01.04 (login by username OR email)
-- and creates case-insensitive indices for secure authentication lookups

-- Add username column (nullable for gradual migration)
ALTER TABLE "user" ADD COLUMN username VARCHAR(100);

-- Case-insensitive unique index for email
-- Allows login with any case variant (user@example.com, USER@EXAMPLE.COM, etc.)
CREATE UNIQUE INDEX idx_user_email_lower
  ON "user" (LOWER(email));

-- Case-insensitive unique index for username
-- Partial index (WHERE username IS NOT NULL) avoids conflicts with existing NULL values
CREATE UNIQUE INDEX idx_user_username_lower
  ON "user" (LOWER(username))
  WHERE username IS NOT NULL;

-- Index for failed login lockout queries
-- Improves performance when checking if account is currently locked
CREATE INDEX idx_user_locked_until
  ON "user" (locked_until)
  WHERE locked_until IS NOT NULL;
