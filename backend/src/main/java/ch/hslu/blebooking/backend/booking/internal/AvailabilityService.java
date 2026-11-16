package ch.hslu.blebooking.backend.booking.internal;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.booking.Availability;
import ch.hslu.blebooking.backend.booking.AvailabilityQuery;
import ch.hslu.blebooking.backend.booking.DayAvailability;
import ch.hslu.blebooking.backend.booking.Slot;
import ch.hslu.blebooking.backend.booking.SlotStatus;
import ch.hslu.blebooking.backend.common.config.BookingProperties;
import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;
import ch.hslu.blebooking.backend.resource.ResourceCatalog;
import ch.hslu.blebooking.backend.resource.ResourceDetails;
import ch.hslu.blebooking.backend.resource.WeeklyWindow;

/**
 * Builds the slot grid from the eligible weekly windows (resource module) and the blocking bookings.
 * Compares instants in UTC and renders them in the booking zone only at the end.
 */
@Service
@Transactional(readOnly = true)
class AvailabilityService implements AvailabilityQuery {

	private final ResourceCatalog catalog;
	private final BookingRepository bookingRepository;
	private final BookingProperties properties;
	private final BookingRules rules;

	AvailabilityService(ResourceCatalog catalog, BookingRepository bookingRepository, BookingProperties properties,
			BookingRules rules) {
		this.catalog = catalog;
		this.bookingRepository = bookingRepository;
		this.properties = properties;
		this.rules = rules;
	}

	@Override
	public Availability availability(long resourceId, LocalDate from, LocalDate to, long userId, String role) {
		validateRange(from, to);
		ResourceDetails resource = catalog.findVisible(resourceId, role)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Resource not found"));

		ZoneId zone = rules.zone();
		Duration slot = rules.slot();
		Instant now = Instant.now();

		// Load all blocking bookings for the whole range once. Windows end at the latest at midnight of "to".
		Instant rangeStart = from.atStartOfDay(zone).toInstant();
		Instant rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant();
		List<Booking> blocking = bookingRepository.findByResourceIdAndStatusInAndStartTimeBeforeAndEndTimeAfter(
				resourceId, BookingRules.BLOCKING, rangeEnd, rangeStart);

		List<DayAvailability> days = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			// keyed by start instant, so identical windows (e.g. STUDENT and STAFF for an admin) give one slot
			Map<Instant, Slot> slots = new TreeMap<>();
			for (WeeklyWindow window : resource.windows()) {
				if (window.dayOfWeek() != date.getDayOfWeek()) {
					continue;
				}
				for (LocalTime t : rules.slotStarts(window)) {
					ZonedDateTime start = ZonedDateTime.of(date, t, zone);
					ZonedDateTime end = start.plus(slot);
					Instant startInstant = start.toInstant();
					if (rules.beyondHorizon(startInstant, now) || slots.containsKey(startInstant)) {
						continue;
					}
					SlotStatus status = status(startInstant, end.toInstant(), now, blocking, userId);
					slots.put(startInstant, new Slot(start.toOffsetDateTime(), end.toOffsetDateTime(), status));
				}
			}
			days.add(new DayAvailability(date, List.copyOf(slots.values())));
		}
		return new Availability(resource.id(), zone.getId(), rules.slotMinutes(), from, to, days);
	}

	private void validateRange(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "'from' must not be after 'to'.");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > properties.maxQueryDays()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
					"The range must not be longer than " + properties.maxQueryDays() + " days.");
		}
	}

	private SlotStatus status(Instant start, Instant end, Instant now, List<Booking> blocking, long userId) {
		if (rules.isPast(start, now)) {
			return SlotStatus.PAST;
		}
		SlotStatus status = SlotStatus.AVAILABLE;
		for (Booking b : blocking) {
			if (BookingRules.overlaps(b.getStartTime(), b.getEndTime(), start, end)) {
				if (b.getUserId() == userId) {
					return SlotStatus.BOOKED_BY_ME;
				}
				status = SlotStatus.BOOKED;
			}
		}
		return status;
	}

}
