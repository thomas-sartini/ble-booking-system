-- =========================================================================
-- T-049: the overlap constraint applies only to blocking bookings (D10)
-- =========================================================================

-- V1 created an unnamed exclusion constraint over all rows, so a cancelled
-- booking would block its slot forever. PostgreSQL named it as below.
ALTER TABLE booking DROP CONSTRAINT booking_resource_id_tstzrange_excl;

-- Explicit name, so the 23P01 handling (T-053) can rely on it.
ALTER TABLE booking ADD CONSTRAINT booking_active_overlap_excl
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    ) WHERE (status IN ('BOOKED', 'CHECKED_IN'));

ALTER TABLE booking ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX idx_booking_user_start ON booking (user_id, start_time);
CREATE INDEX idx_booking_resource_start ON booking (resource_id, start_time);
CREATE INDEX idx_availability_resource ON availability (resource_id);
