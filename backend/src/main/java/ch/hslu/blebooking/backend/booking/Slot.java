package ch.hslu.blebooking.backend.booking;

import java.time.OffsetDateTime;

/**
 * One slot of the availability grid. Times are rendered in the booking zone, e.g. 2026-11-17T08:00:00+01:00.
 */
public record Slot(OffsetDateTime start, OffsetDateTime end, SlotStatus status) {
}
