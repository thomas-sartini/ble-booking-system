package ch.hslu.blebooking.backend.booking;

import java.time.LocalDate;
import java.util.List;

/**
 * Slot grid of a resource between two local dates, both inclusive, with one entry per date.
 */
public record Availability(long resourceId, String timeZone, int slotMinutes, LocalDate from, LocalDate to,
		List<DayAvailability> days) {
}
