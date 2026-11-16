package ch.hslu.blebooking.backend.booking.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.booking.BookingCancelled;
import ch.hslu.blebooking.backend.booking.BookingConfirmed;
import ch.hslu.blebooking.backend.booking.BookingDto;
import ch.hslu.blebooking.backend.booking.BookingResource;
import ch.hslu.blebooking.backend.booking.BookingService;
import ch.hslu.blebooking.backend.booking.BookingStatus;
import ch.hslu.blebooking.backend.booking.CreateBookingRequest;
import ch.hslu.blebooking.backend.booking.OwnedBooking;
import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;
import ch.hslu.blebooking.backend.resource.ResourceCatalog;
import ch.hslu.blebooking.backend.resource.ResourceDetails;
import ch.hslu.blebooking.backend.resource.ResourceSummary;
import ch.hslu.blebooking.backend.resource.WeeklyWindow;
import ch.hslu.blebooking.backend.user.UserAuthService;
import ch.hslu.blebooking.backend.user.UserStatus;

/**
 * Creates, lists and cancels bookings (docs/api/booking-api.md §4). Overlaps are prevented by the exclusion
 * constraint {@code booking_active_overlap_excl} (V4), not by a prior SELECT, so concurrent requests cannot
 * both succeed.
 */
@Service
class BookingServiceImpl implements BookingService {

	/** SQLState of an exclusion constraint violation in PostgreSQL. */
	private static final String EXCLUSION_VIOLATION = "23P01";
	private static final String CURRENCY = "CHF";
	private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

	private final BookingRepository bookingRepository;
	private final ResourceCatalog catalog;
	private final UserAuthService userAuthService;
	private final BookingRules rules;
	private final ApplicationEventPublisher events;

	BookingServiceImpl(BookingRepository bookingRepository, ResourceCatalog catalog, UserAuthService userAuthService,
			BookingRules rules, ApplicationEventPublisher events) {
		this.bookingRepository = bookingRepository;
		this.catalog = catalog;
		this.userAuthService = userAuthService;
		this.rules = rules;
		this.events = events;
	}

	// --- create ---

	@Override
	@Transactional
	public BookingDto create(CreateBookingRequest request, long userId, String role) {
		Instant now = Instant.now();

		// 1. account still ACTIVE (a token stays valid for up to 15 minutes after a deactivation)
		boolean active = userAuthService.findById(userId)
			.map(u -> u.status() == UserStatus.ACTIVE)
			.orElse(false);
		if (!active) {
			throw new ApiException(ErrorCode.FORBIDDEN, "Your account is not active.");
		}

		// 2. request on its own: complete, end > start, duration (D7), future and within horizon (D8)
		if (request == null || request.resourceId() == null || request.start() == null || request.end() == null) {
			throw validation("resourceId, start and end are required.");
		}
		Instant start = request.start().toInstant();
		Instant end = request.end().toInstant();
		if (!end.isAfter(start)) {
			throw validation("end must be after start.");
		}
		Duration duration = Duration.between(start, end);
		if (duration.compareTo(rules.slot()) < 0) {
			throw validation("A booking must last at least " + rules.slotMinutes() + " minutes.");
		}
		if (duration.compareTo(rules.maxDuration()) > 0) {
			throw validation("A booking must not last longer than " + rules.maxDuration().toMinutes() + " minutes.");
		}
		if (rules.isPast(start, now)) {
			throw validation("The start must be in the future.");
		}
		if (rules.beyondHorizon(start, now)) {
			throw validation("The start is too far in the future.");
		}

		// 3. resource visible to the user (D4)
		ResourceDetails resource = catalog.findVisible(request.resourceId(), role)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Resource not found"));

		// 4. + 5. slot grid of the matching window (D6) and inside one eligible window on one day (D3, D7)
		checkWindow(resource.windows(), start, end, duration);

		// 6. no blocking overlap (D10), enforced by the database
		Booking booking = new Booking(userId, resource.id(), start, end, price(resource, duration));
		try {
			booking = bookingRepository.saveAndFlush(booking);
		}
		catch (DataIntegrityViolationException e) {
			if (hasSqlState(e, EXCLUSION_VIOLATION)) {
				throw new ApiException(ErrorCode.BOOKING_OVERLAP, "The resource is already booked at this time.");
			}
			throw e;
		}

		events.publishEvent(new BookingConfirmed(booking.getId()));
		return toDto(booking, new BookingResource(resource.id(), resource.name(), resource.type(),
				resource.position()), now);
	}

	/**
	 * A window matches if it is on the start's weekday and contains the start. The start must lie on that
	 * window's grid and the duration must be a multiple of the slot size, so the end is on the grid as well.
	 */
	private void checkWindow(List<WeeklyWindow> windows, Instant start, Instant end, Duration duration) {
		ZonedDateTime localStart = start.atZone(rules.zone());
		ZonedDateTime localEnd = end.atZone(rules.zone());
		LocalDate date = localStart.toLocalDate();
		LocalTime startTime = localStart.toLocalTime();

		List<WeeklyWindow> matching = windows.stream()
			.filter(w -> w.dayOfWeek() == date.getDayOfWeek())
			.filter(w -> !startTime.isBefore(w.start()) && startTime.isBefore(w.end()))
			.toList();
		if (matching.isEmpty()) {
			throw outsideAvailability();
		}

		boolean wholeSlots = duration.toNanos() % rules.slot().toNanos() == 0;
		List<WeeklyWindow> onGrid = matching.stream()
			.filter(w -> wholeSlots && rules.onGrid(w, startTime))
			.toList();
		if (onGrid.isEmpty()) {
			throw validation("Start and end must lie on the " + rules.slotMinutes() + "-minute slot grid.");
		}

		boolean inside = localEnd.toLocalDate().equals(date)
				&& onGrid.stream().anyMatch(w -> !localEnd.toLocalTime().isAfter(w.end()));
		if (!inside) {
			throw outsideAvailability();
		}
	}

	/** D11: price per hour x duration in hours, HALF_UP to 2 decimals. */
	private static BigDecimal price(ResourceDetails resource, Duration duration) {
		return new BigDecimal(resource.pricePerHour())
			.multiply(BigDecimal.valueOf(duration.toMinutes()))
			.divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
	}

	private static boolean hasSqlState(Throwable e, String sqlState) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof SQLException sql && sqlState.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

	// --- read ---

	@Override
	@Transactional(readOnly = true)
	public List<BookingDto> list(long userId, String scope) {
		Instant now = Instant.now();
		Map<Boolean, List<Booking>> byUpcoming = bookingRepository.findByUserId(userId).stream()
			.collect(Collectors.partitioningBy(b -> isUpcoming(b, now)));
		List<Booking> upcoming = byUpcoming.get(true).stream()
			.sorted(Comparator.comparing(Booking::getStartTime).thenComparing(Booking::getId))
			.toList();
		List<Booking> past = byUpcoming.get(false).stream()
			.sorted(Comparator.comparing(Booking::getStartTime).thenComparing(Booking::getId).reversed())
			.toList();

		List<Booking> result = switch (scope == null ? "" : scope) {
			case "upcoming" -> upcoming;
			case "past" -> past;
			case "all" -> Stream.concat(upcoming.stream(), past.stream()).toList();
			default -> throw validation("scope must be one of: upcoming, past, all.");
		};
		return toDtos(result, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BookingDto get(long bookingId, long userId) {
		Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId).orElseThrow(BookingServiceImpl::notFound);
		return toDtos(List.of(booking), Instant.now()).getFirst();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<OwnedBooking> findById(long bookingId) {
		return bookingRepository.findById(bookingId)
			.map(b -> new OwnedBooking(b.getUserId(), toDtos(List.of(b), Instant.now()).getFirst()));
	}

	/** Upcoming = not ended yet and still blocking (contract §4, GET /bookings). */
	private static boolean isUpcoming(Booking b, Instant now) {
		return b.getEndTime().isAfter(now) && BookingRules.BLOCKING.contains(b.getStatus());
	}

	// --- cancel ---

	@Override
	@Transactional
	public void cancel(long bookingId, long userId) {
		// the row lock serialises concurrent cancellations, so only one of them publishes an event
		Booking booking = bookingRepository.findForUpdate(bookingId, userId).orElseThrow(BookingServiceImpl::notFound);
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			return; // D12: idempotent, no change and no second event
		}
		if (!isCancellable(booking, Instant.now())) {
			throw new ApiException(ErrorCode.INVALID_STATUS, "This booking can no longer be cancelled.");
		}
		booking.cancel();
		events.publishEvent(new BookingCancelled(booking.getId()));
	}

	/** D12: status BOOKED and the start has not been reached. */
	private boolean isCancellable(Booking b, Instant now) {
		return b.getStatus() == BookingStatus.BOOKED && !rules.isPast(b.getStartTime(), now);
	}

	// --- mapping ---

	private List<BookingDto> toDtos(List<Booking> bookings, Instant now) {
		Map<Long, ResourceSummary> resources = catalog
			.findByIds(bookings.stream().map(Booking::getResourceId).distinct().toList()).stream()
			.collect(Collectors.toMap(ResourceSummary::id, Function.identity()));
		return bookings.stream()
			.map(b -> {
				ResourceSummary r = resources.get(b.getResourceId());
				// cannot be missing: deleting a resource deletes its bookings (ON DELETE CASCADE)
				BookingResource resource = r == null
						? new BookingResource(b.getResourceId(), null, null, null)
						: new BookingResource(r.id(), r.name(), r.type(), r.position());
				return toDto(b, resource, now);
			})
			.toList();
	}

	private BookingDto toDto(Booking b, BookingResource resource, Instant now) {
		return new BookingDto(b.getId(), resource, local(b.getStartTime()), local(b.getEndTime()), b.getStatus(),
				b.getLockedPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(), CURRENCY,
				isCancellable(b, now), local(b.getCreatedAt()));
	}

	private OffsetDateTime local(Instant instant) {
		return instant.atZone(rules.zone()).toOffsetDateTime();
	}

	private static ApiException validation(String detail) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, detail);
	}

	private static ApiException outsideAvailability() {
		return new ApiException(ErrorCode.OUTSIDE_AVAILABILITY, "This time is not bookable for you.");
	}

	private static ApiException notFound() {
		return new ApiException(ErrorCode.NOT_FOUND, "Booking not found");
	}

}
