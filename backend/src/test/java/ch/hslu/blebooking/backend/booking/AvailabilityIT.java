package ch.hslu.blebooking.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.TestData;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * GET /resources/{id}/availability (T-049, T-050) and the V4 exclusion constraint.
 * Dates are relative to today in Europe/Zurich. No @Transactional, so real commits are tested.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AvailabilityIT {

	private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;
	private LocalDate today;

	private long me;
	private long other;
	/** Windows 08:00-10:00 every day for STUDENT. */
	private long resource;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		data.deleteAll();
		today = LocalDate.now(ZONE);

		me = data.user("me", "STUDENT");
		other = data.user("other", "STUDENT");
		resource = data.resource("Room", null, "FIXED", "10", "ACTIVE", null);
		for (int day = 0; day <= 6; day++) {
			data.window(resource, day, "08:00", "10:00", "STUDENT");
		}
	}

	// --- grid ---

	@Test
	void responseHasContractShapeAndExactTimestamps() throws Exception {
		LocalDate date = today.plusDays(1);
		availability(resource, date, date, "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resourceId").value(resource))
			.andExpect(jsonPath("$.timeZone").value("Europe/Zurich"))
			.andExpect(jsonPath("$.slotMinutes").value(30))
			.andExpect(jsonPath("$.from").value(date.toString()))
			.andExpect(jsonPath("$.to").value(date.toString()))
			.andExpect(jsonPath("$.days.length()").value(1))
			.andExpect(jsonPath("$.days[0].date").value(date.toString()))
			.andExpect(jsonPath("$.days[0].slots.length()").value(4))
			.andExpect(jsonPath("$.days[0].slots[*].start").value(contains(
					iso(date, "08:00"), iso(date, "08:30"), iso(date, "09:00"), iso(date, "09:30"))))
			.andExpect(jsonPath("$.days[0].slots[0].start").value(
					matchesPattern(date + "T08:00:00\\+0[12]:00")))
			.andExpect(jsonPath("$.days[0].slots[3].end").value(iso(date, "10:00")))
			.andExpect(jsonPath("$.days[0].slots[*].status").value(everyItem(is("AVAILABLE"))));
	}

	@Test
	void dayOfWeekZeroIsMonday() throws Exception {
		long mondayOnly = data.resource("Monday Room", null, "FIXED", "0", "ACTIVE", null);
		data.window(mondayOnly, 0, "08:00", "09:00", "STUDENT");
		LocalDate from = today.plusDays(1);
		LocalDate to = from.plusDays(6);

		ResultActions result = availability(mondayOnly, from, to, "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days.length()").value(7));
		for (int i = 0; i < 7; i++) {
			LocalDate date = from.plusDays(i);
			int expected = date.getDayOfWeek() == DayOfWeek.MONDAY ? 2 : 0;
			result.andExpect(jsonPath("$.days[" + i + "].date").value(date.toString()))
				.andExpect(jsonPath("$.days[" + i + "].slots.length()").value(expected));
		}
	}

	@Test
	void slotStatusesFromBlockingBookings() throws Exception {
		LocalDate date = today.plusDays(1);
		data.booking(other, resource, at(date, "08:00"), at(date, "08:30"), "BOOKED");
		data.booking(other, resource, at(date, "08:30"), at(date, "09:00"), "CHECKED_IN");
		data.booking(me, resource, at(date, "09:00"), at(date, "09:30"), "BOOKED");

		availability(resource, date, date, "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days[0].slots[*].status")
				.value(contains("BOOKED", "BOOKED", "BOOKED_BY_ME", "AVAILABLE")));
	}

	@Test
	void slotsOfYesterdayArePast() throws Exception {
		LocalDate yesterday = today.minusDays(1);
		data.booking(other, resource, at(yesterday, "08:00"), at(yesterday, "09:00"), "COMPLETED");

		availability(resource, yesterday, yesterday, "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days[0].slots.length()").value(4))
			.andExpect(jsonPath("$.days[0].slots[*].status").value(everyItem(is("PAST"))));
	}

	@Test
	void pastTakesPrecedenceOverBooked() throws Exception {
		LocalDate yesterday = today.minusDays(1);
		data.booking(me, resource, at(yesterday, "08:00"), at(yesterday, "10:00"), "BOOKED");

		availability(resource, yesterday, yesterday, "STUDENT", me)
			.andExpect(jsonPath("$.days[0].slots[*].status").value(everyItem(is("PAST"))));
	}

	@Test
	void cancelledCompletedAndNoShowDoNotBlock() throws Exception {
		LocalDate date = today.plusDays(2);
		// overlapping rows are allowed because none of them is blocking (V4)
		data.booking(other, resource, at(date, "08:00"), at(date, "10:00"), "CANCELLED");
		data.booking(other, resource, at(date, "08:00"), at(date, "10:00"), "COMPLETED");
		data.booking(other, resource, at(date, "08:00"), at(date, "10:00"), "NO_SHOW");

		availability(resource, date, date, "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days[0].slots.length()").value(4))
			.andExpect(jsonPath("$.days[0].slots[*].status").value(everyItem(is("AVAILABLE"))));
	}

	@Test
	void cancelledBookingDoesNotPreventNewBookingOfSameInterval() throws Exception {
		LocalDate date = today.plusDays(3);
		data.booking(other, resource, at(date, "08:00"), at(date, "09:00"), "CANCELLED");
		data.booking(other, resource, at(date, "08:00"), at(date, "09:00"), "BOOKED");

		availability(resource, date, date, "STUDENT", me)
			.andExpect(jsonPath("$.days[0].slots[*].status")
				.value(contains("BOOKED", "BOOKED", "AVAILABLE", "AVAILABLE")));
	}

	@Test
	void adjacentBookingsDoNotBlockNeighbourSlots() throws Exception {
		LocalDate date = today.plusDays(4);
		data.booking(other, resource, at(date, "08:30"), at(date, "09:00"), "BOOKED");

		availability(resource, date, date, "STUDENT", me)
			.andExpect(jsonPath("$.days[0].slots[*].status")
				.value(contains("AVAILABLE", "BOOKED", "AVAILABLE", "AVAILABLE")));
	}

	@Test
	void adjacentBlockingBookingsAreAllowedButOverlappingAreRejected() {
		LocalDate date = today.plusDays(5);
		data.booking(other, resource, at(date, "08:00"), at(date, "08:30"), "BOOKED");
		data.booking(me, resource, at(date, "08:30"), at(date, "09:00"), "CHECKED_IN");

		assertThatThrownBy(() -> data.booking(me, resource, at(date, "08:15"), at(date, "08:45"), "BOOKED"))
			.isInstanceOf(DataAccessException.class)
			.satisfies(e -> {
				Throwable cause = ((DataAccessException) e).getMostSpecificCause();
				assertThat(cause).isInstanceOf(SQLException.class);
				assertThat(((SQLException) cause).getSQLState()).isEqualTo("23P01");
				assertThat(cause.getMessage()).contains("booking_active_overlap_excl");
			});
	}

	@Test
	void slotsBeyondHorizonAreNotReturned() throws Exception {
		LocalDate from = today.plusDays(29);
		LocalDate to = today.plusDays(33);

		availability(resource, from, to, "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days.length()").value(5))
			// day 29 lies completely inside now + 30 days
			.andExpect(jsonPath("$.days[0].slots.length()").value(4))
			// day 31 and later start after now + 30 days
			.andExpect(jsonPath("$.days[2].slots.length()").value(0))
			.andExpect(jsonPath("$.days[3].slots.length()").value(0))
			.andExpect(jsonPath("$.days[4].slots.length()").value(0));
	}

	@Test
	void adminGetsNoDuplicateSlotsForIdenticalWindowsOfDifferentRoles() throws Exception {
		long shared = data.resource("Shared", null, "MOBILE", "1", "ACTIVE", null);
		LocalDate date = today.plusDays(1);
		int day = date.getDayOfWeek().getValue() - 1;
		data.window(shared, day, "08:00", "09:00", "STUDENT");
		data.window(shared, day, "08:00", "09:00", "STAFF");
		long admin = data.user("admin", "ADMIN");

		availability(shared, date, date, "ADMIN", admin)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days[0].slots[*].start").value(contains(iso(date, "08:00"), iso(date, "08:30"))));
	}

	@Test
	void onlyEligibleWindowsProduceSlots() throws Exception {
		long mixed = data.resource("Mixed", null, "FIXED", "1", "ACTIVE", null);
		LocalDate date = today.plusDays(1);
		int day = date.getDayOfWeek().getValue() - 1;
		data.window(mixed, day, "08:00", "09:00", "STUDENT");
		data.window(mixed, day, "14:00", "15:00", "STAFF");

		availability(mixed, date, date, "STUDENT", me)
			.andExpect(jsonPath("$.days[0].slots[*].start").value(contains(iso(date, "08:00"), iso(date, "08:30"))));
	}

	// --- query validation ---

	@Test
	void fourteenDaysAreAllowed() throws Exception {
		availability(resource, today.plusDays(1), today.plusDays(14), "STUDENT", me)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.days.length()").value(14));
	}

	@Test
	void fifteenDaysAreRejected() throws Exception {
		expectValidationError("?from=" + today.plusDays(1) + "&to=" + today.plusDays(15));
	}

	@Test
	void fromAfterToIsRejected() throws Exception {
		expectValidationError("?from=" + today.plusDays(2) + "&to=" + today.plusDays(1));
	}

	@Test
	void missingFromIsRejected() throws Exception {
		expectValidationError("?to=" + today);
	}

	@Test
	void missingToIsRejected() throws Exception {
		expectValidationError("?from=" + today);
	}

	@Test
	void malformedDateIsRejected() throws Exception {
		expectValidationError("?from=17.11.2026&to=" + today);
	}

	// --- not found and security ---

	@Test
	void inactiveResourceIsNotFound() throws Exception {
		long inactive = data.resource("Old", null, "MOBILE", "1", "INACTIVE", null);
		data.window(inactive, 0, "08:00", "10:00", "STUDENT");
		expectNotFound(inactive, "ADMIN");
	}

	@Test
	void notEligibleResourceIsNotFound() throws Exception {
		expectNotFound(resource, "STAFF");
	}

	@Test
	void unknownResourceIsNotFound() throws Exception {
		expectNotFound(resource + 1000, "ADMIN");
	}

	@Test
	void withoutTokenIs401() throws Exception {
		mvc.perform(get("/resources/" + resource + "/availability?from=" + today + "&to=" + today))
			.andExpect(status().isUnauthorized());
	}

	private void expectValidationError(String query) throws Exception {
		mvc.perform(get("/resources/" + resource + "/availability" + query)
				.header(HttpHeaders.AUTHORIZATION, bearer("STUDENT", me)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private void expectNotFound(long id, String role) throws Exception {
		LocalDate date = today.plusDays(1);
		availability(id, date, date, role, me)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	private ResultActions availability(long id, LocalDate from, LocalDate to, String role, long userId)
			throws Exception {
		return mvc.perform(get("/resources/" + id + "/availability?from=" + from + "&to=" + to)
			.header(HttpHeaders.AUTHORIZATION, bearer(role, userId)));
	}

	private String bearer(String role, long userId) {
		return "Bearer " + TestJwt.token(secret, role, String.valueOf(userId));
	}

	private static Instant at(LocalDate date, String time) {
		return ZonedDateTime.of(date, LocalTime.parse(time), ZONE).toInstant();
	}

	/** Expected JSON form, e.g. 2026-11-17T08:00:00+01:00. */
	private static String iso(LocalDate date, String time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.of(date, LocalTime.parse(time), ZONE));
	}

}
