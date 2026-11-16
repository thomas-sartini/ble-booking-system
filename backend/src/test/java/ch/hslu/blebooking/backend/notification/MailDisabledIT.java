package ch.hslu.blebooking.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.EventPublications;
import ch.hslu.blebooking.backend.common.TestData;

/**
 * APP_MAIL_ENABLED=false (the Railway default): a reachable SMTP server receives nothing, the listener logs the
 * skip and the publication completes, so nothing piles up in event_publication.
 */
@SpringBootTest(properties = {
		"app.mail.enabled=false",
		"spring.mail.host=localhost",
		"spring.mail.port=3026" })
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class MailDisabledIT {

	@RegisterExtension
	static final GreenMailExtension greenMail = new GreenMailExtension(
			new ServerSetup(3026, null, ServerSetup.PROTOCOL_SMTP));

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
	void disabledMailSendsNothingAndCompletesThePublication(CapturedOutput output) throws Exception {
		long id = fixtures.book(mvc, secret, "09:00", "10:00");

		publications.awaitAllCompleted();
		assertThat(publications.count()).isEqualTo(1);
		assertThat(publications.all().getFirst().get("status")).isEqualTo("COMPLETED");
		await().atMost(Duration.ofSeconds(5))
			.until(() -> output.getAll().contains("mail disabled, skipped booking " + id));
		assertThat(greenMail.getReceivedMessages()).isEmpty();
	}

}
