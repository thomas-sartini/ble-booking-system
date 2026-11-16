package ch.hslu.blebooking.backend.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import ch.hslu.blebooking.backend.common.TestData;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * Shared fixture for the mail tests that only need "a user books a resource".
 */
final class MailTestFixtures {

	static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

	final long userId;
	final long resourceId;
	final LocalDate day = LocalDate.now(ZONE).plusDays(2);

	MailTestFixtures(TestData data) {
		userId = data.user("mailuser", "STUDENT");
		resourceId = data.resource("Mail Room", null, "FIXED", "10.00", "ACTIVE", null);
		for (int d = 0; d <= 6; d++) {
			data.window(resourceId, d, "08:00", "12:00", "STUDENT");
		}
	}

	String bearer(String secret) {
		return "Bearer " + TestJwt.token(secret, "STUDENT", String.valueOf(userId));
	}

	long book(MockMvc mvc, String secret, String start, String end) throws Exception {
		MvcResult result = mvc.perform(post("/bookings")
				.header(HttpHeaders.AUTHORIZATION, bearer(secret))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"resourceId\": " + resourceId + ", \"start\": \"" + iso(start) + "\", \"end\": \""
						+ iso(end) + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private String iso(String time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.of(day, LocalTime.parse(time), ZONE));
	}

}
