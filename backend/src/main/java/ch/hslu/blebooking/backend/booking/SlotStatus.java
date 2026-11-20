package ch.hslu.blebooking.backend.booking;

/**
 * Status of a slot in the availability grid.
 */
public enum SlotStatus {

	/** Free and bookable. */
	AVAILABLE,
	/** Blocked by another user's booking. */
	BOOKED,
	/** Blocked by the current user's own booking. */
	BOOKED_BY_ME,
	/** Start is now or in the past. */
	PAST

}
