package ch.hslu.blebooking.backend.booking;

import java.time.LocalDate;

/**
 * Public API for the availability grid of a resource.
 */
public interface AvailabilityQuery {

	/**
	 * Slot grid for the windows the user is eligible for.
	 * @param resourceId resource id
	 * @param from first local date, inclusive
	 * @param to last local date, inclusive
	 * @param userId current user id (JWT "sub"), used for BOOKED_BY_ME
	 * @param role current user role (JWT "role")
	 * @throws ch.hslu.blebooking.backend.common.error.ApiException VALIDATION_ERROR for an invalid range,
	 *         NOT_FOUND if the resource is not visible to the user
	 */
	Availability availability(long resourceId, LocalDate from, LocalDate to, long userId, String role);

}
