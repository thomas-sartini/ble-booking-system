package ch.hslu.blebooking.backend.resource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * GET /resources and GET /resources/{id} (T-049, T-050): visibility per role (D3, D4), filters, details.
 * No @Transactional, so real commits are tested.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ResourceCatalogIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;
	private TestData data;

	private long bike;
	private long room;
	private long lab;
	private long empty;
	private long inactive;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		data = new TestData(jdbc);
		data.deleteAll();

		// inserted out of name order on purpose
		lab = data.resource("Charlie Lab", "3D printers", "FIXED", "12.5", "ACTIVE", "Building C, Room 1.05");
		data.window(lab, 1, "09:00", "17:00", "STAFF");
		data.window(lab, 0, "13:00", "15:00", "STAFF");
		data.window(lab, 0, "08:00", "12:00", "STAFF");

		bike = data.resource("Alpha Bike", "Electric cargo bike", "MOBILE", "5", "ACTIVE", "Building A");
		for (String role : new String[] { "STUDENT", "STAFF" }) {
			data.window(bike, 6, "08:00", "18:00", role);
			data.window(bike, 0, "08:00", "18:00", role);
		}

		room = data.resource("Beta Room", null, "FIXED", "0", "ACTIVE", null);
		data.window(room, 2, "07:00", "21:00", "STUDENT");

		empty = data.resource("Delta Empty", "No windows", "MOBILE", "1", "ACTIVE", "Building D");

		inactive = data.resource("Echo Old", "Retired", "MOBILE", "2", "INACTIVE", "Building E");
		data.window(inactive, 0, "08:00", "18:00", "STUDENT");
		data.window(inactive, 0, "08:00", "18:00", "STAFF");
	}

	// --- GET /resources ---

	@Test
	void studentSeesOnlyResourcesWithStudentWindows() throws Exception {
		list("STUDENT", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(contains("Alpha Bike", "Beta Room")));
	}

	@Test
	void staffSeesOnlyResourcesWithStaffWindows() throws Exception {
		list("STAFF", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(contains("Alpha Bike", "Charlie Lab")));
	}

	@Test
	void adminSeesAllActiveResourcesWithWindowsSortedByName() throws Exception {
		list("ADMIN", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(contains("Alpha Bike", "Beta Room", "Charlie Lab")));
	}

	@Test
	void summaryHasContractFields() throws Exception {
		list("STAFF", "?q=charlie")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(lab))
			.andExpect(jsonPath("$[0].name").value("Charlie Lab"))
			.andExpect(jsonPath("$[0].description").value("3D printers"))
			.andExpect(jsonPath("$[0].type").value("FIXED"))
			.andExpect(jsonPath("$[0].position").value("Building C, Room 1.05"))
			.andExpect(jsonPath("$[0].pricePerHour").value("12.50"));
	}

	@Test
	void nullDescriptionAndPositionAreReturnedAsNull() throws Exception {
		list("STUDENT", "?type=FIXED")
			.andExpect(jsonPath("$[0].name").value("Beta Room"))
			.andExpect(content().string(containsString("\"description\":null")))
			.andExpect(content().string(containsString("\"position\":null")))
			.andExpect(jsonPath("$[0].pricePerHour").value("0.00"));
	}

	@Test
	void filterByType() throws Exception {
		list("ADMIN", "?type=FIXED")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(contains("Beta Room", "Charlie Lab")));
		list("ADMIN", "?type=MOBILE")
			.andExpect(jsonPath("$[*].name").value(contains("Alpha Bike")));
	}

	@Test
	void searchIsCaseInsensitiveOverNameDescriptionAndPosition() throws Exception {
		list("ADMIN", "?q=LAB").andExpect(jsonPath("$[*].name").value(contains("Charlie Lab")));
		list("ADMIN", "?q=electric").andExpect(jsonPath("$[*].name").value(contains("Alpha Bike")));
		list("ADMIN", "?q=building c").andExpect(jsonPath("$[*].name").value(contains("Charlie Lab")));
		list("ADMIN", "?q=nothing-matches").andExpect(jsonPath("$").value(empty()));
	}

	@Test
	void searchDoesNotRevealHiddenResources() throws Exception {
		list("ADMIN", "?q=delta").andExpect(jsonPath("$").value(empty()));
		list("ADMIN", "?q=echo").andExpect(jsonPath("$").value(empty()));
		list("STUDENT", "?q=charlie").andExpect(jsonPath("$").value(empty()));
	}

	@Test
	void typeAndSearchCombine() throws Exception {
		list("ADMIN", "?type=MOBILE&q=beta").andExpect(jsonPath("$").value(empty()));
		list("ADMIN", "?type=FIXED&q=beta").andExpect(jsonPath("$[*].name").value(contains("Beta Room")));
	}

	@Test
	void invalidTypeIsValidationError() throws Exception {
		list("ADMIN", "?type=BOAT")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void listWithoutTokenIs401() throws Exception {
		mvc.perform(get("/resources")).andExpect(status().isUnauthorized());
	}

	// --- GET /resources/{id} ---

	@Test
	void detailsHaveContractFieldsAndSortedEligibleWindows() throws Exception {
		details("STAFF", lab)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(lab))
			.andExpect(jsonPath("$.name").value("Charlie Lab"))
			.andExpect(jsonPath("$.type").value("FIXED"))
			.andExpect(jsonPath("$.pricePerHour").value("12.50"))
			.andExpect(jsonPath("$.timeZone").value("Europe/Zurich"))
			.andExpect(jsonPath("$.windows.length()").value(3))
			.andExpect(jsonPath("$.windows[0].dayOfWeek").value("MONDAY"))
			.andExpect(jsonPath("$.windows[0].start").value("08:00"))
			.andExpect(jsonPath("$.windows[0].end").value("12:00"))
			.andExpect(jsonPath("$.windows[1].dayOfWeek").value("MONDAY"))
			.andExpect(jsonPath("$.windows[1].start").value("13:00"))
			.andExpect(jsonPath("$.windows[2].dayOfWeek").value("TUESDAY"))
			.andExpect(jsonPath("$.windows[2].start").value("09:00"))
			.andExpect(jsonPath("$.windows[2].end").value("17:00"));
	}

	@Test
	void dayOfWeekZeroIsMondayAndSixIsSunday() throws Exception {
		details("STUDENT", bike)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.windows[*].dayOfWeek").value(contains("MONDAY", "SUNDAY")));
	}

	@Test
	void adminSeesIdenticalWindowsOfDifferentRolesOnce() throws Exception {
		details("ADMIN", bike)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.windows.length()").value(2))
			.andExpect(jsonPath("$.windows[*].dayOfWeek").value(contains("MONDAY", "SUNDAY")));
	}

	@Test
	void notEligibleResourceIsNotFound() throws Exception {
		details("STUDENT", lab)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		details("STAFF", room)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void inactiveResourceIsNotFoundEvenForAdmin() throws Exception {
		details("ADMIN", inactive)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void resourceWithoutWindowsIsNotFound() throws Exception {
		details("ADMIN", empty)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void unknownResourceIsNotFound() throws Exception {
		details("ADMIN", inactive + 1000)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void detailsWithoutTokenIs401() throws Exception {
		mvc.perform(get("/resources/" + bike)).andExpect(status().isUnauthorized());
	}

	private ResultActions list(String role, String query) throws Exception {
		return mvc.perform(get("/resources" + query).header(HttpHeaders.AUTHORIZATION, bearer(role)));
	}

	private ResultActions details(String role, long id) throws Exception {
		return mvc.perform(get("/resources/" + id).header(HttpHeaders.AUTHORIZATION, bearer(role)));
	}

	private String bearer(String role) {
		return "Bearer " + TestJwt.token(secret, role);
	}

}
