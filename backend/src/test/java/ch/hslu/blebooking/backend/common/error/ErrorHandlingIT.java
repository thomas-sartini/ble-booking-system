package ch.hslu.blebooking.backend.common.error;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.TestEndpoints;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * Checks that errors are returned as Problem Details (RFC 9457) with a "code" field
 * and without internal details.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, TestEndpoints.class })
class ErrorHandlingIT {

	@Autowired
	private WebApplicationContext context;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;

	private String token;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		token = "Bearer " + TestJwt.token(secret, "STUDENT");
	}

	@Test
	void apiExceptionBecomesProblemDetail() throws Exception {
		mvc.perform(get("/test/api-error").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("BOOKING_OVERLAP"))
			.andExpect(jsonPath("$.detail").value("The resource is already booked at this time."));
	}

	@Test
	void invalidInputReturnsValidationError() throws Exception {
		mvc.perform(post("/test/validation").header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void unknownPathReturnsNotFound() throws Exception {
		mvc.perform(get("/test/does-not-exist").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void unexpectedErrorHidesInternalDetails() throws Exception {
		mvc.perform(get("/test/unexpected").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.trace").doesNotExist())
			.andExpect(content().string(not(containsString("internal detail"))));
	}

}
