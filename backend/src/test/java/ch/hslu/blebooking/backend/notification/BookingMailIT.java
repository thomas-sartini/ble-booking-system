package ch.hslu.blebooking.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jayway.jsonpath.JsonPath;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.EventPublications;
import ch.hslu.blebooking.backend.common.TestData;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * Booking emails end to end (T-054, T-059): HTTP request -> commit -> event publication -> async listener ->
 * SMTP (GreenMail). No @Transactional, so the after-commit listeners really run.
 */
@SpringBootTest(properties = {
		"app.mail.enabled=true",
		"app.mail.from=no-reply@ble-booking.test",
		"spring.mail.host=localhost",
		"spring.mail.port=3025" })
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class BookingMailIT {

	@RegisterExtension
	static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
		.withPerMethodLifecycle(false);

	private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
	private static final String PASSWORD = "secret-test-password";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;
	private EventPublications publications;
	private LocalDate day;

	private long anna;
	private long other;
	/** 12.50/h, every day STUDENT 08:00-12:00. */
	private long bike;

	@BeforeEach
	void setUp() throws Exception {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		publications = new EventPublications(jdbc);
		publications.awaitSettled();  // listeners of the previous test must not deliver into this one
		data.deleteAll();
		greenMail.purgeEmailFromAllMailboxes();
		day = LocalDate.now(ZONE).plusDays(2);

		anna = data.user("anna", "STUDENT", passwordEncoder.encode(PASSWORD));
		data.userFirstName(anna, "Anna");
		other = data.user("other", "STUDENT");
		bike = data.resource("Cargo Bike", null, "MOBILE", "12.50", "ACTIVE", "Building A, bike shelter");
		for (int d = 0; d <= 6; d++) {
			data.window(bike, d, "08:00", "12:00", "STUDENT");
		}
	}

	// --- confirmation ---

	@Test
	void successfulBookingSendsExactlyOneConfirmationToTheOwner() throws Exception {
		long id = createBooking(anna, "09:00", "10:30");

		MimeMessage mail = awaitMails(1)[0];
		publications.awaitAllCompleted();
		assertThat(greenMail.getReceivedMessages()).hasSize(1);

		assertThat(mail.getRecipients(Message.RecipientType.TO))
			.extracting(a -> ((InternetAddress) a).getAddress())
			.containsExactly("anna@test.com");
		assertThat(((InternetAddress) mail.getFrom()[0]).getAddress()).isEqualTo("no-reply@ble-booking.test");
		assertThat(mail.getSubject()).isEqualTo("Booking confirmed: Cargo Bike on " + longDate(day));

		String body = body(mail);
		assertThat(body)
			.contains("Hello Anna,")
			.contains("your booking is confirmed.")
			.contains("Resource:   Cargo Bike")
			.contains("Location:   Building A, bike shelter")
			.contains("Date:       " + longDate(day))
			.contains("Time:       09:00–10:30 (Europe/Zurich)")
			.contains("Price:      CHF 18.75")
			.contains("Booking ID: " + id);

		Map<String, Object> publication = publications.all().getFirst();
		assertThat(publication.get("event_type").toString()).endsWith("BookingConfirmed");
		assertThat(publication.get("serialized_event").toString()).isEqualTo("{\"bookingId\":" + id + "}");
		assertThat(publication.get("status")).isEqualTo("COMPLETED");
	}

	@Test
	void rejectedBookingsSendNothing() throws Exception {
		data.booking(other, bike, at("09:00"), at("10:00"), "BOOKED");

		book(anna, "09:00", "10:00").andExpect(status().isConflict());   // BOOKING_OVERLAP
		book(anna, "11:00", "13:00").andExpect(status().isConflict());   // OUTSIDE_AVAILABILITY
		book(anna, "10:00", "10:15").andExpect(status().isBadRequest()); // VALIDATION_ERROR
		mvc.perform(post("/bookings").header(HttpHeaders.AUTHORIZATION, bearer(anna))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(bike + 1000, "10:00", "11:00")))
			.andExpect(status().isNotFound());

		// no event, so nothing can be delivered later either
		assertThat(publications.count()).isZero();
		assertThat(greenMail.getReceivedMessages()).isEmpty();
	}

	// --- cancellation ---

	@Test
	void cancellationSendsExactlyOneMailAndTheRepeatSendsNothing() throws Exception {
		long id = createBooking(anna, "09:00", "10:00");
		awaitMails(1);
		publications.awaitAllCompleted();

		cancel(anna, id).andExpect(status().isNoContent());
		MimeMessage cancellation = awaitMails(2)[1];
		publications.awaitAllCompleted();

		assertThat(cancellation.getSubject()).isEqualTo("Booking cancelled: Cargo Bike on " + longDate(day));
		assertThat(((InternetAddress) cancellation.getRecipients(Message.RecipientType.TO)[0]).getAddress())
			.isEqualTo("anna@test.com");
		assertThat(body(cancellation))
			.contains("Hello Anna,")
			.contains("your booking has been cancelled.")
			.contains("Time:       09:00–10:00 (Europe/Zurich)")
			.contains("Booking ID: " + id);

		cancel(anna, id).andExpect(status().isNoContent());
		assertThat(publications.count()).isEqualTo(2);
		publications.awaitAllCompleted();
		assertThat(greenMail.getReceivedMessages()).hasSize(2);
	}

	@Test
	void rejectedCancellationsSendNothing() throws Exception {
		long othersBooking = data.booking(other, bike, at("09:00"), at("10:00"), "BOOKED");
		long checkedIn = data.booking(anna, bike, at("10:00"), at("11:00"), "CHECKED_IN");

		cancel(anna, othersBooking).andExpect(status().isNotFound());
		cancel(anna, checkedIn).andExpect(status().isConflict());
		cancel(anna, checkedIn + 1000).andExpect(status().isNotFound());

		assertThat(publications.count()).isZero();
		assertThat(greenMail.getReceivedMessages()).isEmpty();
	}

	// --- content ---

	@Test
	void mailContainsNoTokensPasswordsOrOtherUsersData() throws Exception {
		MvcResult login = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"identifier\": \"anna\", \"password\": \"" + PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
		String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");
		data.booking(other, bike, at("08:00"), at("09:00"), "BOOKED");

		mvc.perform(post("/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(bike, "10:00", "11:00")))
			.andExpect(status().isCreated());
		MimeMessage mail = awaitMails(1)[0];

		String all = mail.getSubject() + "\n" + body(mail);
		assertThat(all)
			.doesNotContain(accessToken)
			.doesNotContain(refreshToken)
			.doesNotContain(PASSWORD)
			.doesNotContain("$argon2")
			.doesNotContainIgnoringCase("token")
			.doesNotContainIgnoringCase("password")
			.doesNotContain("other@test.com")
			.doesNotContain("http");
	}

	// --- helpers ---

	private long createBooking(long userId, String start, String end) throws Exception {
		MvcResult result = book(userId, start, end).andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private ResultActions book(long userId, String start, String end) throws Exception {
		return mvc.perform(post("/bookings").header(HttpHeaders.AUTHORIZATION, bearer(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(bike, start, end)));
	}

	private ResultActions cancel(long userId, long bookingId) throws Exception {
		return mvc.perform(delete("/bookings/" + bookingId).header(HttpHeaders.AUTHORIZATION, bearer(userId)));
	}

	private String json(long resourceId, String start, String end) {
		return "{\"resourceId\": " + resourceId + ", \"start\": \"" + iso(start) + "\", \"end\": \"" + iso(end)
				+ "\"}";
	}

	private String bearer(long userId) {
		return "Bearer " + TestJwt.token(secret, "STUDENT", String.valueOf(userId));
	}

	private static MimeMessage[] awaitMails(int count) {
		await().atMost(Duration.ofSeconds(15)).until(() -> greenMail.getReceivedMessages().length >= count);
		return greenMail.getReceivedMessages();
	}

	private static String body(MimeMessage mail) throws Exception {
		return mail.getContent().toString().replace("\r\n", "\n");
	}

	private java.time.Instant at(String time) {
		return ZonedDateTime.of(day, LocalTime.parse(time), ZONE).toInstant();
	}

	private String iso(String time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.of(day, LocalTime.parse(time), ZONE));
	}

	private static String longDate(LocalDate date) {
		return DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH).format(date);
	}

}
