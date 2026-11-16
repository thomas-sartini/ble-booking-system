package ch.hslu.blebooking.backend.booking;

/**
 * A booking together with the id of its owner. Only for other modules (e.g. notifications),
 * never returned by a user-facing endpoint.
 */
public record OwnedBooking(long ownerId, BookingDto booking) {
}
