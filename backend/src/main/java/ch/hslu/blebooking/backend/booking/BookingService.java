package ch.hslu.blebooking.backend.booking;

import java.util.List;
import java.util.Optional;

/**
 * Public API for creating, reading and cancelling the current user's bookings (contract §4).
 * Errors are thrown as {@link ch.hslu.blebooking.backend.common.error.ApiException}.
 */
public interface BookingService {

	/**
	 * Creates a booking in status BOOKED and publishes {@link BookingConfirmed}.
	 * Errors in the order of the contract: FORBIDDEN, VALIDATION_ERROR, NOT_FOUND, VALIDATION_ERROR (slot grid),
	 * OUTSIDE_AVAILABILITY, BOOKING_OVERLAP.
	 */
	BookingDto create(CreateBookingRequest request, long userId, String role);

	/**
	 * The user's bookings.
	 * @param scope "upcoming", "past" or "all"; anything else is a VALIDATION_ERROR
	 */
	List<BookingDto> list(long userId, String scope);

	/** One of the user's bookings, NOT_FOUND if unknown or owned by someone else. */
	BookingDto get(long bookingId, long userId);

	/**
	 * Cancels one of the user's bookings and publishes {@link BookingCancelled}. Does nothing if it is
	 * already CANCELLED. NOT_FOUND if unknown or not owned, INVALID_STATUS if cancelling is not allowed (D12).
	 */
	void cancel(long bookingId, long userId);

	/**
	 * Any booking by id, without ownership check. Only for other modules such as notifications, which act on
	 * behalf of the owner; never use it for user-facing endpoints (D13).
	 */
	Optional<OwnedBooking> findById(long bookingId);

}
