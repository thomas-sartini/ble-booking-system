package ch.hslu.blebooking.backend.booking;

/**
 * Published after a booking was really cancelled, not on an idempotent repeat (contract §6, D12).
 */
public record BookingCancelled(long bookingId) {
}
