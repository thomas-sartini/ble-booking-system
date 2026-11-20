package ch.hslu.blebooking.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.TestData;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * POST /bookings (T-052, T-053): every rule of the validation order in contract §4, price (D11),
 * persistence and the BookingConfirmed event. Dates are relative to today in Europe/Zurich.
 * No @Transactional, so real commits are tested.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
class BookingCreationIT {

	private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
	private static final String PASSWORD = "test-password";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ApplicationEvents events;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;
	private LocalDate day;

	private long me;
	private long other;
	private long staff;
	private long admin;
	/** 12.50/h. Every day: STUDENT 08:00-12:00 and 13:00-18:00, STAFF 18:00-20:00. */
	private long room;
	/** Every day STAFF 08:00-12:00 only. */
	private long lab;
	/** Every day STUDENT 08:15-10:15, so its grid is :15/:45. */
	private long oddRoom;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		data.deleteAll();
		day = LocalDate.now(ZONE).plusDays(2);

		me = data.user("me", "STUDENT");
		other = data.user("other", "STUDENT");
		staff = data.user("staff", "STAFF");
		admin = data.user("admin", "ADMIN");

		room = data.resource("Room", null, "FIXED", "12.50", "ACTIVE", "Building D, Room 2.14");
		lab = data.resource("Lab", null, "FIXED", "5.00", "ACTIVE", null);
		oddRoom = data.resource("Odd Room", null, "FIXED", "1.00", "ACTIVE", null);
		for (int d = 0; d <= 6; d++) {
			data.window(room, d, "08:00", "12:00", "STUDENT");
			data.window(room, d, "13:00", "18:00", "STUDENT");
			data.window(room, d, "18:00", "20:00", "STAFF");
			data.window(lab, d, "08:00", "12:00", "STAFF");
			data.window(oddRoom, d, "08:15", "10:15", "STUDENT");
		}
	}

	// --- success ---

	@Test
	void createsBookingWithContractShape() throws Exception {
		MvcResult result = book(me, "STUDENT", room, iso(day, "09:00"), iso(day, "10:30"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.resource.id").value(room))
			.andExpect(jsonPath("$.resource.name").value("Room"))
			.andExpect(jsonPath("$.resource.type").value("FIXED"))
			.andExpect(jsonPath("$.resource.position").value("Building D, Room 2.14"))
			.andExpect(jsonPath("$.start").value(iso(day, "09:00")))
			.andExpect(jsonPath("$.end").value(iso(day, "10:30")))
			.andExpect(jsonPath("$.status").value("BOOKED"))
			.andExpect(jsonPath("$.price").value("18.75"))
			.andExpect(jsonPath("$.currency").value("CHF"))
			.andExpect(jsonPath("$.cancellable").value(true))
			.andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*[+-]\\d{2}:\\d{2}")))
			.andReturn();

		long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/bookings/" + id);

		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM booking WHERE id = ?", id);
		assertThat(((Number) row.get("user_id")).longValue()).isEqualTo(me);
		assertThat(((Number) row.get("resource_id")).longValue()).isEqualTo(room);
		assertThat(row.get("status")).isEqualTo("BOOKED");
		assertThat(row.get("locked_price").toString()).isEqualTo("18.75");
		assertThat(((Timestamp) row.get("start_time")).toInstant()).isEqualTo(at(day, "09:00").toInstant());
		assertThat(((Timestamp) row.get("end_time")).toInstant()).isEqualTo(at(day, "10:30").toInstant());
		assertThat(row.get("created_at")).isNotNull();

		assertThat(events.stream(BookingConfirmed.class)).containsExactly(new BookingConfirmed(id));
	}

	@Test
	void acceptsAnyOffsetAndAnswersInZurichTime() throws Exception {
		String startUtc = at(day, "09:00").withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString();
		String endUtc = at(day, "09:30").withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString();
		assertThat(startUtc).endsWith("Z");

		book(me, "STUDENT", room, startUtc, endUtc)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.start").value(iso(day, "09:00")))
			.andExpect(jsonPath("$.end").value(iso(day, "09:30")));
	}

	@Test
	void staffAndAdminCanBookTheStaffWindow() throws Exception {
		book(staff, "STAFF", room, iso(day, "18:00"), iso(day, "18:30")).andExpect(status().isCreated());
		book(admin, "ADMIN", room, iso(day, "19:00"), iso(day, "19:30")).andExpect(status().isCreated());
		book(admin, "ADMIN", lab, iso(day, "08:00"), iso(day, "09:00")).andExpect(status().isCreated());
	}

	@Test
	void slotGridIsRelativeToTheWindowStart() throws Exception {
		book(me, "STUDENT", oddRoom, iso(day, "08:15"), iso(day, "08:45")).andExpect(status().isCreated());
	}

	// --- price (D11) ---

	@Test
	void priceIsPricePerHourTimesHoursRoundedHalfUp() throws Exception {
		long free = data.resource("Free", null, "FIXED", "0.00", "ACTIVE", null);
		long cheap = data.resource("Cheap", null, "MOBILE", "0.33", "ACTIVE", null);
		int weekday = day.getDayOfWeek().getValue() - 1;
		data.window(free, weekday, "08:00", "12:00", "STUDENT");
		data.window(cheap, weekday, "08:00", "12:00", "STUDENT");

		book(me, "STUDENT", room, iso(day, "08:00"), iso(day, "12:00"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.price").value("50.00"));
		book(me, "STUDENT", free, iso(day, "08:00"), iso(day, "09:00"))
			.andExpect(jsonPath("$.price").value("0.00"));
		// 0.33 x 0.5 = 0.165 -> 0.17
		book(me, "STUDENT", cheap, iso(day, "08:00"), iso(day, "08:30"))
			.andExpect(jsonPath("$.price").value("0.17"));
	}

	// --- rule 1: account ACTIVE -> 403 ---

	@Test
	void userDisabledAfterLoginGets403AndNoBooking() throws Exception {
		data.user("soon-disabled", "STUDENT", passwordEncoder.encode(PASSWORD));
		MvcResult login = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"identifier\": \"soon-disabled\", \"password\": \"" + PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
		String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
		long userId = ((Number) JsonPath.read(login.getResponse().getContentAsString(), "$.user.id")).longValue();

		data.userStatus(userId, "DISABLED");

		mvc.perform(post("/bookings")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(room, iso(day, "09:00"), iso(day, "10:00"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		assertNothingCreated();
	}

	@Test
	void accountCheckComesBeforeBodyValidation() throws Exception {
		data.userStatus(me, "DISABLED");
		postJson(me, "STUDENT", "{}")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		assertNothingCreated();
	}

	// --- rule 2: request on its own -> 400 ---

	@Test
	void missingFieldsAreRejected() throws Exception {
		expectValidationError(postJson(me, "STUDENT", "{}"));
		expectValidationError(postJson(me, "STUDENT", "{\"start\": \"" + iso(day, "09:00") + "\", \"end\": \""
				+ iso(day, "10:00") + "\"}"));
		expectValidationError(postJson(me, "STUDENT", "{\"resourceId\": " + room + ", \"end\": \""
				+ iso(day, "10:00") + "\"}"));
		expectValidationError(postJson(me, "STUDENT", "{\"resourceId\": " + room + ", \"start\": \""
				+ iso(day, "09:00") + "\"}"));
	}

	@Test
	void malformedJsonIsRejected() throws Exception {
		expectValidationError(postJson(me, "STUDENT", "{not json"));
		expectValidationError(postJson(me, "STUDENT", body(room, "tomorrow at nine", iso(day, "10:00"))));
	}

	@Test
	void endMustBeAfterStart() throws Exception {
		expectValidationError(book(me, "STUDENT", room, iso(day, "10:00"), iso(day, "10:00")));
		expectValidationError(book(me, "STUDENT", room, iso(day, "10:00"), iso(day, "09:00")));
	}

	@Test
	void shorterThanOneSlotIsRejected() throws Exception {
		expectValidationError(book(me, "STUDENT", room, iso(day, "09:00"), iso(day, "09:15")));
	}

	@Test
	void longerThanMaxDurationIsRejected() throws Exception {
		expectValidationError(book(me, "STUDENT", room, iso(day, "13:00"), iso(day, "17:30")));
		book(me, "STUDENT", room, iso(day, "13:00"), iso(day, "17:00")).andExpect(status().isCreated());
	}

	@Test
	void startInThePastIsRejected() throws Exception {
		LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
		expectValidationError(book(me, "STUDENT", room, iso(yesterday, "09:00"), iso(yesterday, "10:00")));
	}

	@Test
	void startBeyondHorizonIsRejected() throws Exception {
		LocalDate far = LocalDate.now(ZONE).plusDays(31);
		expectValidationError(book(me, "STUDENT", room, iso(far, "09:00"), iso(far, "10:00")));

		LocalDate inside = LocalDate.now(ZONE).plusDays(29);
		book(me, "STUDENT", room, iso(inside, "09:00"), iso(inside, "10:00")).andExpect(status().isCreated());
	}

	// --- rule 3: resource visible -> 404 ---

	@Test
	void unknownResourceIsNotFound() throws Exception {
		expectNotFound(book(me, "STUDENT", oddRoom + 1000, iso(day, "09:00"), iso(day, "10:00")));
	}

	@Test
	void inactiveResourceIsNotFound() throws Exception {
		data.resourceStatus(room, "INACTIVE");
		expectNotFound(book(admin, "ADMIN", room, iso(day, "09:00"), iso(day, "10:00")));
	}

	@Test
	void resourceOfAnotherRoleIsNotFound() throws Exception {
		expectNotFound(book(me, "STUDENT", lab, iso(day, "09:00"), iso(day, "10:00")));
	}

	// --- rule 4: slot grid of the matching window -> 400 ---

	@Test
	void startOffTheWindowGridIsRejected() throws Exception {
		expectValidationError(book(me, "STUDENT", room, iso(day, "08:10"), iso(day, "08:40")));
		expectValidationError(book(me, "STUDENT", oddRoom, iso(day, "08:30"), iso(day, "09:00")));
	}

	@Test
	void durationThatIsNotAMultipleOfTheSlotIsRejected() throws Exception {
		expectValidationError(book(me, "STUDENT", room, iso(day, "09:00"), iso(day, "09:45")));
	}

	@Test
	void secondsAreOffTheGrid() throws Exception {
		expectValidationError(book(me, "STUDENT", room, iso(day, "09:00:30"), iso(day, "09:30:30")));
	}

	// --- rule 5: inside one eligible window -> 409 OUTSIDE_AVAILABILITY ---

	@Test
	void windowOfAnotherRoleIsOutsideAvailability() throws Exception {
		expectOutsideAvailability(book(me, "STUDENT", room, iso(day, "18:00"), iso(day, "18:30")));
	}

	@Test
	void spanningTwoWindowsIsOutsideAvailability() throws Exception {
		expectOutsideAvailability(book(me, "STUDENT", room, iso(day, "11:30"), iso(day, "13:30")));
	}

	@Test
	void partlyOutsideTheWindowIsOutsideAvailability() throws Exception {
		// 17:30-18:30: the window ends at 18:00, 18:00-20:00 is for STAFF only
		expectOutsideAvailability(book(me, "STUDENT", room, iso(day, "17:30"), iso(day, "18:30")));
	}

	@Test
	void timeBetweenWindowsIsOutsideAvailability() throws Exception {
		expectOutsideAvailability(book(me, "STUDENT", room, iso(day, "12:00"), iso(day, "12:30")));
		expectOutsideAvailability(book(me, "STUDENT", room, iso(day, "07:00"), iso(day, "07:30")));
	}

	@Test
	void dayWithoutWindowIsOutsideAvailability() throws Exception {
		long weekdayOnly = data.resource("One Day", null, "FIXED", "1.00", "ACTIVE", null);
		data.window(weekdayOnly, day.getDayOfWeek().getValue() - 1, "08:00", "12:00", "STUDENT");
		LocalDate nextDay = day.plusDays(1);
		expectOutsideAvailability(book(me, "STUDENT", weekdayOnly, iso(nextDay, "09:00"), iso(nextDay, "10:00")));
	}

	// --- rule 6: no blocking overlap -> 409 BOOKING_OVERLAP ---

	@Test
	void overlapWithBlockingBookingsIsRejected() throws Exception {
		data.booking(other, room, at(day, "09:00").toInstant(), at(day, "10:00").toInstant(), "BOOKED");
		data.booking(me, room, at(day, "13:00").toInstant(), at(day, "14:00").toInstant(), "CHECKED_IN");
		int before = data.bookingCount();

		expectOverlap(book(me, "STUDENT", room, iso(day, "09:00"), iso(day, "10:00")));   // same interval
		expectOverlap(book(me, "STUDENT", room, iso(day, "09:30"), iso(day, "10:30")));   // partial
		expectOverlap(book(me, "STUDENT", room, iso(day, "08:00"), iso(day, "11:00")));   // contains it
		expectOverlap(book(me, "STUDENT", room, iso(day, "13:30"), iso(day, "14:00")));   // own CHECKED_IN
		assertThat(data.bookingCount()).isEqualTo(before);
		assertThat(events.stream(BookingConfirmed.class)).isEmpty();
	}

	@Test
	void adjacentBookingsAreAllowed() throws Exception {
		data.booking(other, room, at(day, "09:00").toInstant(), at(day, "10:00").toInstant(), "BOOKED");

		book(me, "STUDENT", room, iso(day, "08:00"), iso(day, "09:00")).andExpect(status().isCreated());
		book(me, "STUDENT", room, iso(day, "10:00"), iso(day, "10:30")).andExpect(status().isCreated());
		assertThat(data.bookingCount()).isEqualTo(3);
		assertThat(events.stream(BookingConfirmed.class)).hasSize(2);
	}

	@Test
	void nonBlockingBookingsDoNotPreventANewBooking() throws Exception {
		for (String status : new String[] { "CANCELLED", "COMPLETED", "NO_SHOW" }) {
			data.booking(other, room, at(day, "09:00").toInstant(), at(day, "10:00").toInstant(), status);
		}
		book(me, "STUDENT", room, iso(day, "09:00"), iso(day, "10:00")).andExpect(status().isCreated());
	}

	// --- validation order ---

	@Test
	void pastStartOnUnknownResourceIsValidationError() throws Exception {
		LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
		expectValidationError(book(me, "STUDENT", oddRoom + 1000, iso(yesterday, "09:00"), iso(yesterday, "10:00")));
	}

	@Test
	void offGridOnInvisibleResourceIsNotFound() throws Exception {
		expectNotFound(book(me, "STUDENT", lab, iso(day, "08:10"), iso(day, "08:40")));
	}

	@Test
	void offGridComesBeforeOverlap() throws Exception {
		data.booking(other, oddRoom, at(day, "08:15").toInstant(), at(day, "09:15").toInstant(), "BOOKED");
		expectValidationError(book(me, "STUDENT", oddRoom, iso(day, "08:30"), iso(day, "09:00")));
	}

	@Test
	void outsideAvailabilityComesBeforeOverlap() throws Exception {
		// a blocking row outside any window (e.g. created before the windows changed)
		data.booking(other, room, at(day, "12:00").toInstant(), at(day, "13:00").toInstant(), "BOOKED");
		expectOutsideAvailability(book(me, "STUDENT", room, iso(day, "12:00"), iso(day, "12:30")));
	}

	@Test
	void withoutTokenIs401() throws Exception {
		mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON)
				.content(body(room, iso(day, "09:00"), iso(day, "10:00"))))
			.andExpect(status().isUnauthorized());
		assertThat(data.bookingCount()).isZero();
	}

	// --- helpers ---

	private void expectValidationError(ResultActions result) throws Exception {
		expectError(result, 400, "VALIDATION_ERROR");
	}

	private void expectNotFound(ResultActions result) throws Exception {
		expectError(result, 404, "NOT_FOUND");
	}

	private void expectOutsideAvailability(ResultActions result) throws Exception {
		expectError(result, 409, "OUTSIDE_AVAILABILITY");
	}

	private void expectOverlap(ResultActions result) throws Exception {
		expectError(result, 409, "BOOKING_OVERLAP");
	}

	private void expectError(ResultActions result, int status, String code) throws Exception {
		result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
		assertThat(events.stream(BookingConfirmed.class)).isEmpty();
	}

	/** No booking was created by the API (fixtures inserted with SQL are not counted). */
	private void assertNothingCreated() {
		assertThat(data.bookingCount()).isZero();
		assertThat(events.stream(BookingConfirmed.class)).isEmpty();
	}

	private ResultActions book(long userId, String role, long resourceId, String start, String end)
			throws Exception {
		return postJson(userId, role, body(resourceId, start, end));
	}

	private ResultActions postJson(long userId, String role, String json) throws Exception {
		return mvc.perform(post("/bookings")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwt.token(secret, role, String.valueOf(userId)))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private static String body(long resourceId, String start, String end) {
		return "{\"resourceId\": " + resourceId + ", \"start\": \"" + start + "\", \"end\": \"" + end + "\"}";
	}

	private static ZonedDateTime at(LocalDate date, String time) {
		return ZonedDateTime.of(LocalDateTime.of(date, LocalTime.parse(time)), ZONE);
	}

	/** ISO-8601 with the Zurich offset, e.g. 2026-11-18T09:00:00+01:00. */
	private static String iso(LocalDate date, String time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(at(date, time));
	}

}
