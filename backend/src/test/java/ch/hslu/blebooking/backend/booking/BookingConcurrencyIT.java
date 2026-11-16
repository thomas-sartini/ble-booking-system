package ch.hslu.blebooking.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.TestData;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * Two users book the same slot at the same moment (T-053, FR-04.02). The exclusion constraint must let exactly
 * one of them succeed. Repeated for several slots so the result does not depend on one lucky timing.
 * No @Transactional, so the two requests really run in separate, concurrent transactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class BookingConcurrencyIT {

	private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
	private static final int ROUNDS = 5;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;
	private ExecutorService executor;

	private long alice;
	private long bob;
	private long room;
	private LocalDate day;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		data.deleteAll();
		executor = Executors.newFixedThreadPool(2);
		day = LocalDate.now(ZONE).plusDays(1);

		alice = data.user("alice", "STUDENT");
		bob = data.user("bob", "STUDENT");
		room = data.resource("Room", null, "FIXED", "10.00", "ACTIVE", null);
		data.window(room, day.getDayOfWeek().getValue() - 1, "08:00", "18:00", "STUDENT");
	}

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void concurrentRequestsForTheSameSlotCreateExactlyOneBooking() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			LocalTime start = LocalTime.of(8, 0).plusMinutes(60L * round);
			String body = "{\"resourceId\": " + room + ", \"start\": \"" + iso(start) + "\", \"end\": \""
					+ iso(start.plusMinutes(30)) + "\"}";

			CyclicBarrier barrier = new CyclicBarrier(2);
			List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
			for (long user : new long[] { alice, bob }) {
				String token = TestJwt.token(secret, "STUDENT", String.valueOf(user));
				futures.add(executor.submit(() -> {
					barrier.await(10, TimeUnit.SECONDS);
					return mvc.perform(post("/bookings")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
						.andReturn()
						.getResponse();
				}));
			}

			List<MockHttpServletResponse> responses = new ArrayList<>();
			for (Future<MockHttpServletResponse> future : futures) {
				responses.add(future.get(30, TimeUnit.SECONDS));
			}

			List<Integer> statuses = responses.stream().map(MockHttpServletResponse::getStatus).sorted().toList();
			assertThat(statuses).as("round %d", round).containsExactly(201, 409);
			MockHttpServletResponse conflict = responses.stream().filter(r -> r.getStatus() == 409).findFirst()
				.orElseThrow();
			String code = JsonPath.read(conflict.getContentAsString(), "$.code");
			assertThat(code).isEqualTo("BOOKING_OVERLAP");

			Integer rows = jdbc.queryForObject(
					"SELECT COUNT(*) FROM booking WHERE resource_id = ? AND start_time = ?", Integer.class, room,
					ZonedDateTime.of(day, start, ZONE).toOffsetDateTime());
			assertThat(rows).as("round %d", round).isEqualTo(1);
		}
		assertThat(data.bookingCount()).isEqualTo(ROUNDS);
	}

	private String iso(LocalTime time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.of(day, time, ZONE));
	}

}
