package ch.hslu.blebooking.backend.booking;

/**
 * Published after a booking was created (contract §6). Listeners load what they need by id.
 */
public record BookingConfirmed(long bookingId) {
}
