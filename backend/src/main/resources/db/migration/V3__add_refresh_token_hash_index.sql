-- =========================================================================
-- BLE Booking System - Add Refresh Token Hash Index (Task T-034)
-- =========================================================================
-- Adds unique index on token_hash for security and performance
-- Ensures no two active tokens can have the same hash (defense in depth)
-- Improves lookup performance during refresh and logout operations

-- Unique index on token_hash
-- Ensures each refresh token hash is unique in the database
CREATE UNIQUE INDEX idx_refresh_token_hash
  ON refresh_token (token_hash);
