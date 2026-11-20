package ch.hslu.blebooking.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.EventPublications;
import ch.hslu.blebooking.backend.common.TestData;

/**
 * SMTP is unreachable (FR-07.02): the booking and the cancellation stay, the event publication stays incomplete
 * (so it is re-submitted on restart) and the log names the booking id but no address or mail content.
 */
@SpringBootTest(properties = {
		"app.mail.enabled=true",
		"spring.mail.host=localhost",
		"spring.mail.port=1" })  // nothing listens on port 1
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class BookingMailFailureIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;
	private EventPublications publications;
	private MailTestFixtures fixtures;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		publications = new EventPublications(jdbc);
		publications.awaitSettled();
		data.deleteAll();
		fixtures = new MailTestFixtures(data);
	}

	@Test
	void failedConfirmationKeepsTheBookingAndLeavesThePublicationIncomplete(CapturedOutput output) throws Exception {
		long id = fixtures.book(mvc, secret, "09:00", "10:00");

		Map<String, Object> publication = awaitFailedPublication();
		assertThat(publication.get("event_type").toString()).endsWith("BookingConfirmed");
		assertThat(publication.get("completion_date")).isNull();
		assertThat(data.bookingStatus(id)).isEqualTo("BOOKED");

		await().atMost(Duration.ofSeconds(5))
			.until(() -> output.getAll().contains("Could not send CONFIRMED mail for booking " + id));
		assertThat(output.getAll()).doesNotContain("mailuser@test.com");
	}

	@Test
	void failedCancellationMailKeepsTheCancellation(CapturedOutput output) throws Exception {
		long id = fixtures.book(mvc, secret, "10:00", "11:00");
		awaitFailedPublication();

		mvc.perform(delete("/bookings/" + id).header(HttpHeaders.AUTHORIZATION, fixtures.bearer(secret)))
			.andExpect(status().isNoContent());

		await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.queryForObject("""
				SELECT COUNT(*) FROM event_publication
				WHERE event_type LIKE '%BookingCancelled' AND status = 'FAILED' AND completion_date IS NULL
				""", Integer.class) == 1);
		assertThat(data.bookingStatus(id)).isEqualTo("CANCELLED");
		assertThat(publications.incomplete()).isEqualTo(2);
		assertThat(output.getAll()).contains("Could not send CANCELLED mail for booking " + id)
			.doesNotContain("mailuser@test.com");
	}

	private Map<String, Object> awaitFailedPublication() {
		await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.queryForObject(
				"SELECT COUNT(*) FROM event_publication WHERE status = 'FAILED'", Integer.class) >= 1);
		return publications.all().getFirst();
	}

}
