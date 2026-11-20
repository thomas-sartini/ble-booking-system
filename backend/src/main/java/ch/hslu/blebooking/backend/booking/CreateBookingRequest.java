package ch.hslu.blebooking.backend.booking;

import java.time.OffsetDateTime;

/**
 * Body of POST /bookings. Times may have any offset or Z. Completeness is checked by the service,
 * because the account check comes first in the validation order (contract §4).
 */
public record CreateBookingRequest(Long resourceId, OffsetDateTime start, OffsetDateTime end) {
}
