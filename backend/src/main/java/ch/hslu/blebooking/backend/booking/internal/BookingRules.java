package ch.hslu.blebooking.backend.booking.internal;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ch.hslu.blebooking.backend.booking.BookingStatus;
import ch.hslu.blebooking.backend.common.config.BookingProperties;
import ch.hslu.blebooking.backend.resource.WeeklyWindow;

/**
 * Booking rules shared by the availability grid and booking creation (docs/api/booking-api.md, D6-D10),
 * so both always agree on zone, slot grid, past, horizon and overlap.
 */
@Component
class BookingRules {

	/** D10: only these statuses block a slot. */
	static final List<BookingStatus> BLOCKING = List.of(BookingStatus.BOOKED, BookingStatus.CHECKED_IN);

	private final BookingProperties properties;

	BookingRules(BookingProperties properties) {
		this.properties = properties;
	}

	ZoneId zone() {
		return properties.zone();
	}

	int slotMinutes() {
		return properties.slotMinutes();
	}

	Duration slot() {
		return Duration.ofMinutes(properties.slotMinutes());
	}

	Duration maxDuration() {
		return properties.maxDuration();
	}

	/** D6: slot starts of a window, window start + n x slot, as long as the slot ends inside the window. */
	List<LocalTime> slotStarts(WeeklyWindow window) {
		Duration slot = slot();
		List<LocalTime> starts = new ArrayList<>();
		// the second condition stops at midnight instead of wrapping around to the next day
		for (LocalTime t = window.start(); !t.plus(slot).isAfter(window.end()) && t.plus(slot).isAfter(t);
				t = t.plus(slot)) {
			starts.add(t);
		}
		return starts;
	}

	/** D6: the local time lies on the slot grid of the window (window start + n x slot). */
	boolean onGrid(WeeklyWindow window, LocalTime time) {
		if (time.isBefore(window.start())) {
			return false;
		}
		Duration offset = Duration.between(window.start(), time);
		return offset.toNanos() % slot().toNanos() == 0;
	}

	/** A slot or booking that starts now or earlier is in the past. */
	boolean isPast(Instant start, Instant now) {
		return !start.isAfter(now);
	}

	/** D8: the start lies more than the horizon ahead. */
	boolean beyondHorizon(Instant start, Instant now) {
		return start.isAfter(now.plus(properties.horizon()));
	}

	/** Half-open intervals: adjacent intervals (end = next start) do not overlap. */
	static boolean overlaps(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
		return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
	}

}
