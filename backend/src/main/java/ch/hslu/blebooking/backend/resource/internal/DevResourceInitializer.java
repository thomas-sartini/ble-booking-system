package ch.hslu.blebooking.backend.resource.internal;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.resource.ResourceStatus;
import ch.hslu.blebooking.backend.resource.ResourceType;

/**
 * Creates demo resources with weekly windows (docs/api/booking-api.md, §5 "Dev seed data").
 * Only active in the "dev" profile. Idempotent: a resource is created only if none with the same name exists.
 * A {@code @Service} because only services may use repositories (ArchitectureIT).
 */
@Service
@Profile("dev")
class DevResourceInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevResourceInitializer.class);

	private static final List<String> STUDENT_AND_STAFF = List.of("STUDENT", "STAFF");
	private static final List<String> STAFF_ONLY = List.of("STAFF");

	/** day_of_week values (D2): 0 = Monday ... 6 = Sunday. */
	private static final List<Integer> MON_TO_FRI = List.of(0, 1, 2, 3, 4);
	private static final List<Integer> SATURDAY = List.of(5);

	private record Window(List<Integer> days, LocalTime start, LocalTime end, List<String> roles) {
	}

	private final ResourceRepository resourceRepository;
	private final AvailabilityRepository availabilityRepository;

	DevResourceInitializer(ResourceRepository resourceRepository, AvailabilityRepository availabilityRepository) {
		this.resourceRepository = resourceRepository;
		this.availabilityRepository = availabilityRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		create("Cargo Bike 1", "Electric cargo bike for transporting material", "5.00", ResourceStatus.ACTIVE,
				ResourceType.MOBILE, "Building A, bike shelter",
				List.of(new Window(MON_TO_FRI, LocalTime.of(8, 0), LocalTime.of(18, 0), STUDENT_AND_STAFF)));
		create("Project Room D2.14", "Project room for up to 8 people", "0.00", ResourceStatus.ACTIVE,
				ResourceType.FIXED, "Building D, Room 2.14",
				List.of(new Window(MON_TO_FRI, LocalTime.of(7, 0), LocalTime.of(21, 0), STUDENT_AND_STAFF),
						new Window(SATURDAY, LocalTime.of(9, 0), LocalTime.of(14, 0), STUDENT_AND_STAFF)));
		create("3D Printer Lab", "Lab with 3D printers", "12.50", ResourceStatus.ACTIVE, ResourceType.FIXED,
				"Building C, Room 1.05",
				List.of(new Window(MON_TO_FRI, LocalTime.of(9, 0), LocalTime.of(17, 0), STAFF_ONLY)));
		create("Old Beamer", "Retired projector", "2.00", ResourceStatus.INACTIVE, ResourceType.MOBILE,
				"Building A, storage", List.of());
	}

	private void create(String name, String description, String price, ResourceStatus status, ResourceType type,
			String position, List<Window> windows) {
		if (resourceRepository.existsByName(name)) {
			log.info("Dev resource already exists: {}", name);
			return;
		}
		Resource resource = resourceRepository.save(
				new Resource(name, description, new BigDecimal(price), status, type, position));
		for (Window window : windows) {
			for (int day : window.days()) {
				for (String role : window.roles()) {
					availabilityRepository.save(new Availability(resource.getId(), (short) day, window.start(),
							window.end(), role));
				}
			}
		}
		log.info("Created dev resource: {} ({}, {})", name, type, status);
	}

}
