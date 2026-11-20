package ch.hslu.blebooking.backend.booking;

import java.time.LocalDate;
import java.util.List;

/**
 * Slots of one local date. Empty if the user has no eligible window that day.
 */
public record DayAvailability(LocalDate date, List<Slot> slots) {
}
