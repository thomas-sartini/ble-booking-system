package ch.hslu.blebooking.backend.booking;

import java.time.OffsetDateTime;

/**
 * A booking as returned by the API, the "Booking" shape of the contract (§3). Times are in the booking zone,
 * the price is a string with 2 decimals.
 */
public record BookingDto(long id, BookingResource resource, OffsetDateTime start, OffsetDateTime end,
		BookingStatus status, String price, String currency, boolean cancellable, OffsetDateTime createdAt) {
}
