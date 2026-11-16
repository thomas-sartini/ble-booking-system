package ch.hslu.blebooking.backend.resource.internal;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.common.config.BookingProperties;
import ch.hslu.blebooking.backend.resource.ResourceCatalog;
import ch.hslu.blebooking.backend.resource.ResourceDetails;
import ch.hslu.blebooking.backend.resource.ResourceStatus;
import ch.hslu.blebooking.backend.resource.ResourceSummary;
import ch.hslu.blebooking.backend.resource.ResourceType;
import ch.hslu.blebooking.backend.resource.WeeklyWindow;

/**
 * Implements visibility (D4) and role eligibility (D3) of resources and their weekly windows.
 */
@Service
@Transactional(readOnly = true)
class ResourceCatalogService implements ResourceCatalog {

	private static final String ADMIN = "ADMIN";

	private static final Comparator<WeeklyWindow> WINDOW_ORDER = Comparator
		.comparing(WeeklyWindow::dayOfWeek)
		.thenComparing(WeeklyWindow::start)
		.thenComparing(WeeklyWindow::end);

	private final ResourceRepository resourceRepository;
	private final AvailabilityRepository availabilityRepository;
	private final BookingProperties bookingProperties;

	ResourceCatalogService(ResourceRepository resourceRepository, AvailabilityRepository availabilityRepository,
			BookingProperties bookingProperties) {
		this.resourceRepository = resourceRepository;
		this.availabilityRepository = availabilityRepository;
		this.bookingProperties = bookingProperties;
	}

	@Override
	public List<ResourceSummary> findVisible(String role, ResourceType type, String q) {
		List<Resource> active = resourceRepository.findByStatus(ResourceStatus.ACTIVE).stream()
			.filter(r -> type == null || r.getType() == type)
			.filter(r -> matches(r, q))
			.toList();
		if (active.isEmpty()) {
			return List.of();
		}
		Map<Long, List<Availability>> windowsByResource = availabilityRepository
			.findByResourceIdIn(active.stream().map(Resource::getId).toList()).stream()
			.collect(Collectors.groupingBy(Availability::getResourceId));
		return active.stream()
			.filter(r -> !eligibleWindows(windowsByResource.getOrDefault(r.getId(), List.of()), role).isEmpty())
			.sorted(Comparator.comparing(Resource::getName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(Resource::getId))
			.map(ResourceCatalogService::toSummary)
			.toList();
	}

	@Override
	public Optional<ResourceDetails> findVisible(long id, String role) {
		return resourceRepository.findById(id)
			.filter(r -> r.getStatus() == ResourceStatus.ACTIVE)
			.flatMap(r -> {
				List<WeeklyWindow> windows = eligibleWindows(availabilityRepository.findByResourceId(id), role);
				if (windows.isEmpty()) {
					return Optional.empty();
				}
				return Optional.of(new ResourceDetails(r.getId(), r.getName(), r.getDescription(), r.getType(),
						r.getPosition(), price(r), bookingProperties.zone().getId(), windows));
			});
	}

	/** Windows the role may book in, deduplicated and sorted by day and start. */
	private static List<WeeklyWindow> eligibleWindows(List<Availability> windows, String role) {
		if (role == null) {
			return List.of();
		}
		return windows.stream()
			.filter(w -> ADMIN.equals(role) || role.equals(w.getPermittedRole()))
			// D2: 0 = Monday ... 6 = Sunday
			.map(w -> new WeeklyWindow(DayOfWeek.of(w.getDayOfWeek() + 1), w.getStartTime(), w.getEndTime()))
			.distinct()
			.sorted(WINDOW_ORDER)
			.toList();
	}

	private static boolean matches(Resource r, String q) {
		if (q == null || q.isBlank()) {
			return true;
		}
		String needle = q.strip().toLowerCase(Locale.ROOT);
		return contains(r.getName(), needle) || contains(r.getDescription(), needle)
				|| contains(r.getPosition(), needle);
	}

	private static boolean contains(String value, String needle) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
	}

	private static ResourceSummary toSummary(Resource r) {
		return new ResourceSummary(r.getId(), r.getName(), r.getDescription(), r.getType(), r.getPosition(),
				price(r));
	}

	private static String price(Resource r) {
		return r.getPricePerHour().setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

}
