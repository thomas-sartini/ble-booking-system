package ch.hslu.blebooking.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.TestData;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * GET /bookings, GET /bookings/{id}, DELETE /bookings/{id} (T-057, T-058): scopes and sorting, ownership (D13),
 * cancellation rules (D12), the cancellable flag and the BookingCancelled event.
 * No @Transactional, so real commits are tested.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
class MyBookingsIT {

	private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ApplicationEvents events;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;
	private LocalDate today;

	private long me;
	private long other;
	/** Every day STUDENT 08:00-12:00. */
	private long room;

	// my bookings
	private long inUse;        // CHECKED_IN, now - 30 min .. now + 30 min -> upcoming
	private long tomorrow;     // BOOKED, +1 day 08:00-09:00 -> upcoming
	private long later;        // BOOKED, +3 days 08:00-09:00 -> upcoming
	private long cancelled;    // CANCELLED, +2 days 10:00-11:00 -> past
	private long completed;    // COMPLETED, -1 day -> past
	private long noShow;       // NO_SHOW, -2 days -> past
	private long ended;        // BOOKED but over, -3 days -> past
	// someone else's
	private long othersBooking;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		data.deleteAll();
		today = LocalDate.now(ZONE);

		me = data.user("me", "STUDENT");
		other = data.user("other", "STUDENT");
		room = data.resource("Room", null, "FIXED", "10.00", "ACTIVE", "Building D");
		for (int d = 0; d <= 6; d++) {
			data.window(room, d, "08:00", "12:00", "STUDENT");
		}

		Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		inUse = data.booking(me, room, now.minus(Duration.ofMinutes(30)), now.plus(Duration.ofMinutes(30)),
				"CHECKED_IN");
		tomorrow = booking(me, 1, "08:00", "09:00", "BOOKED");
		later = booking(me, 3, "08:00", "09:00", "BOOKED");
		cancelled = booking(me, 2, "10:00", "11:00", "CANCELLED");
		completed = booking(me, -1, "08:00", "09:00", "COMPLETED");
		noShow = booking(me, -2, "08:00", "09:00", "NO_SHOW");
		ended = booking(me, -3, "08:00", "09:00", "BOOKED");
		othersBooking = booking(other, 1, "10:00", "11:00", "BOOKED");
	}

	// --- GET /bookings ---

	@Test
	void upcomingAreNotEndedAndBlockingSortedByStartAscending() throws Exception {
		list(me, "?scope=upcoming")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id").value(contains(ids(inUse, tomorrow, later))));
	}

	@Test
	void pastIsEverythingElseSortedByStartDescending() throws Exception {
		list(me, "?scope=past")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id").value(contains(ids(cancelled, completed, noShow, ended))));
	}

	@Test
	void allIsUpcomingThenPastAndTheDefault() throws Exception {
		Object[] expected = ids(inUse, tomorrow, later, cancelled, completed, noShow, ended);
		list(me, "?scope=all").andExpect(jsonPath("$[*].id").value(contains(expected)));
		list(me, "").andExpect(jsonPath("$[*].id").value(contains(expected)));
	}

	@Test
	void othersBookingsAreNeverListed() throws Exception {
		list(other, "").andExpect(jsonPath("$[*].id").value(contains(ids(othersBooking))));
	}

	@Test
	void noBookingsIsEmptyList() throws Exception {
		long newcomer = data.user("newcomer", "STUDENT");
		list(newcomer, "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value(empty()));
	}

	@Test
	void invalidScopeIsValidationError() throws Exception {
		list(me, "?scope=UPCOMING")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		list(me, "?scope=future")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void cancellableFlagFollowsD12() throws Exception {
		list(me, "")
			.andExpect(jsonPath("$[0].cancellable").value(false))   // inUse, CHECKED_IN
			.andExpect(jsonPath("$[1].cancellable").value(true))    // tomorrow, BOOKED in the future
			.andExpect(jsonPath("$[2].cancellable").value(true))    // later
			.andExpect(jsonPath("$[3].cancellable").value(false))   // cancelled
			.andExpect(jsonPath("$[4].cancellable").value(false))   // completed
			.andExpect(jsonPath("$[5].cancellable").value(false))   // noShow
			.andExpect(jsonPath("$[6].cancellable").value(false));  // ended, BOOKED but in the past
	}

	@Test
	void withoutTokenIs401() throws Exception {
		mvc.perform(get("/bookings")).andExpect(status().isUnauthorized());
		mvc.perform(get("/bookings/" + tomorrow)).andExpect(status().isUnauthorized());
		mvc.perform(delete("/bookings/" + tomorrow)).andExpect(status().isUnauthorized());
		assertThat(data.bookingStatus(tomorrow)).isEqualTo("BOOKED");
	}

	// --- GET /bookings/{id} ---

	@Test
	void ownBookingHasContractShape() throws Exception {
		getBooking(me, tomorrow)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(tomorrow))
			.andExpect(jsonPath("$.resource.id").value(room))
			.andExpect(jsonPath("$.resource.name").value("Room"))
			.andExpect(jsonPath("$.resource.type").value("FIXED"))
			.andExpect(jsonPath("$.resource.position").value("Building D"))
			.andExpect(jsonPath("$.start").value(iso(today.plusDays(1), "08:00")))
			.andExpect(jsonPath("$.end").value(iso(today.plusDays(1), "09:00")))
			.andExpect(jsonPath("$.status").value("BOOKED"))
			.andExpect(jsonPath("$.price").value("0.00"))
			.andExpect(jsonPath("$.currency").value("CHF"))
			.andExpect(jsonPath("$.cancellable").value(true))
			.andExpect(jsonPath("$.createdAt").isString());
	}

	@Test
	void othersAndUnknownBookingsAreNotFound() throws Exception {
		getBooking(me, othersBooking)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		getBooking(me, othersBooking + 1000)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void adminAlsoOnlySeesOwnBookings() throws Exception {
		long admin = data.user("admin", "ADMIN");
		mvc.perform(get("/bookings/" + tomorrow).header(HttpHeaders.AUTHORIZATION, bearer(admin, "ADMIN")))
			.andExpect(status().isNotFound());
	}

	@Test
	void bookingStillShowsItsResourceAfterTheResourceBecameInactive() throws Exception {
		data.resourceStatus(room, "INACTIVE");
		getBooking(me, tomorrow)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resource.name").value("Room"));
	}

	// --- DELETE /bookings/{id} ---

	@Test
	void cancelsOwnFutureBookingOnce() throws Exception {
		cancel(me, tomorrow).andExpect(status().isNoContent());
		assertThat(data.bookingStatus(tomorrow)).isEqualTo("CANCELLED");
		assertThat(events.stream(BookingCancelled.class)).containsExactly(new BookingCancelled(tomorrow));

		getBooking(me, tomorrow)
			.andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.cancellable").value(false));
	}

	@Test
	void repeatedCancellationChangesNothingAndPublishesNoSecondEvent() throws Exception {
		cancel(me, tomorrow).andExpect(status().isNoContent());
		cancel(me, tomorrow).andExpect(status().isNoContent());
		assertThat(data.bookingStatus(tomorrow)).isEqualTo("CANCELLED");
		assertThat(events.stream(BookingCancelled.class)).hasSize(1);

		// a booking that was already CANCELLED before
		cancel(me, cancelled).andExpect(status().isNoContent());
		assertThat(events.stream(BookingCancelled.class)).hasSize(1);
	}

	@Test
	void checkedInCompletedAndNoShowCannotBeCancelled() throws Exception {
		for (long id : new long[] { inUse, completed, noShow }) {
			String before = data.bookingStatus(id);
			cancel(me, id)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS"));
			assertThat(data.bookingStatus(id)).isEqualTo(before);
		}
		assertThat(events.stream(BookingCancelled.class)).isEmpty();
	}

	@Test
	void bookedBookingCannotBeCancelledAfterItsStart() throws Exception {
		// started but not ended yet
		long otherRoom = data.resource("Other Room", null, "FIXED", "0", "ACTIVE", null);
		Instant now = Instant.now();
		long started = data.booking(me, otherRoom, now.minus(Duration.ofMinutes(10)),
				now.plus(Duration.ofMinutes(20)), "BOOKED");

		for (long id : new long[] { started, ended }) {
			cancel(me, id)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS"));
			assertThat(data.bookingStatus(id)).isEqualTo("BOOKED");
		}
		assertThat(events.stream(BookingCancelled.class)).isEmpty();
	}

	@Test
	void othersAndUnknownBookingsCannotBeCancelled() throws Exception {
		cancel(me, othersBooking)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		assertThat(data.bookingStatus(othersBooking)).isEqualTo("BOOKED");

		cancel(me, othersBooking + 1000)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		assertThat(events.stream(BookingCancelled.class)).isEmpty();
	}

	@Test
	void cancelledBookingFreesItsSlots() throws Exception {
		LocalDate day = today.plusDays(1);
		String availability = "/resources/" + room + "/availability?from=" + day + "&to=" + day;

		mvc.perform(get(availability).header(HttpHeaders.AUTHORIZATION, bearer(other, "STUDENT")))
			.andExpect(jsonPath("$.days[0].slots[0].status").value("BOOKED"))
			.andExpect(jsonPath("$.days[0].slots[1].status").value("BOOKED"));

		cancel(me, tomorrow).andExpect(status().isNoContent());

		mvc.perform(get(availability).header(HttpHeaders.AUTHORIZATION, bearer(other, "STUDENT")))
			.andExpect(jsonPath("$.days[0].slots[0].status").value("AVAILABLE"))
			.andExpect(jsonPath("$.days[0].slots[1].status").value("AVAILABLE"));

		mvc.perform(post("/bookings")
				.header(HttpHeaders.AUTHORIZATION, bearer(other, "STUDENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"resourceId\": " + room + ", \"start\": \"" + iso(day, "08:00") + "\", \"end\": \""
						+ iso(day, "09:00") + "\"}"))
			.andExpect(status().isCreated());
	}

	// --- helpers ---

	private long booking(long userId, int daysFromToday, String start, String end, String status) {
		LocalDate date = today.plusDays(daysFromToday);
		return data.booking(userId, room, at(date, start).toInstant(), at(date, end).toInstant(), status);
	}

	private ResultActions list(long userId, String query) throws Exception {
		return mvc.perform(get("/bookings" + query).header(HttpHeaders.AUTHORIZATION, bearer(userId, "STUDENT")));
	}

	private ResultActions getBooking(long userId, long bookingId) throws Exception {
		return perform(get("/bookings/" + bookingId), userId);
	}

	private ResultActions cancel(long userId, long bookingId) throws Exception {
		return perform(delete("/bookings/" + bookingId), userId);
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, long userId) throws Exception {
		return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(userId, "STUDENT")));
	}

	private String bearer(long userId, String role) {
		return "Bearer " + TestJwt.token(secret, role, String.valueOf(userId));
	}

	/** JSON numbers are compared as Integer by JsonPath for small values. */
	private static Object[] ids(long... ids) {
		Object[] result = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) {
			result[i] = (int) ids[i];
		}
		return result;
	}

	private static ZonedDateTime at(LocalDate date, String time) {
		return ZonedDateTime.of(date, LocalTime.parse(time), ZONE);
	}

	private static String iso(LocalDate date, String time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(at(date, time));
	}

}
