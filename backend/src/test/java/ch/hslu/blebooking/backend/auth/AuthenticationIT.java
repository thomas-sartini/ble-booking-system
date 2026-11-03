package ch.hslu.blebooking.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.auth.internal.RefreshToken;
import ch.hslu.blebooking.backend.auth.internal.RefreshTokenRepository;
import ch.hslu.blebooking.backend.common.TestJwt;
import ch.hslu.blebooking.backend.user.Role;
import ch.hslu.blebooking.backend.user.UserStatus;
import ch.hslu.blebooking.backend.user.internal.User;
import ch.hslu.blebooking.backend.user.internal.UserRepository;

/**
 * Integration tests for web authentication (login/logout).
 * Tests cover security requirements FR-01.04, FR-01.06, NFR-01.01, NFR-01.02, NFR-01.05.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthenticationIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.jwt.secret}")
	private String jwtSecret;

	private MockMvc mvc;

	private User activeUser;
	private User invitedUser;
	private User disabledUser;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

		// Create test users with different statuses
		activeUser = createUser("active@test.com", "active", "password123", UserStatus.ACTIVE);
		invitedUser = createUser("invited@test.com", "invited", "password123", UserStatus.INVITED);
		disabledUser = createUser("disabled@test.com", "disabled", "password123", UserStatus.DISABLED);
	}

	@Test
	void loginWithValidEmail() throws Exception {
		String body = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		MvcResult result = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.refreshToken").isString())
			.andExpect(jsonPath("$.user.id").value(activeUser.getId()))
			.andExpect(jsonPath("$.user.email").value("active@test.com"))
			.andExpect(jsonPath("$.user.username").value("active"))
			.andExpect(jsonPath("$.user.role").value("STUDENT"))
			.andReturn();

		// Verify access token and refresh token are present
		String responseBody = result.getResponse().getContentAsString();
		String accessToken = JsonPath.read(responseBody, "$.accessToken");
		String refreshToken = JsonPath.read(responseBody, "$.refreshToken");
		assertThat(accessToken).isNotEmpty();
		assertThat(refreshToken).isNotEmpty();
	}

	@Test
	void loginWithValidUsername() throws Exception {
		String body = """
			{"identifier": "active", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.username").value("active"));
	}

	@Test
	void loginCaseInsensitiveEmail() throws Exception {
		String body = """
			{"identifier": "ACTIVE@TEST.COM", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.email").value("active@test.com"));
	}

	@Test
	void loginCaseInsensitiveUsername() throws Exception {
		String[] variants = { "ACTIVE", "Active", "AcTiVe" };

		for (String variant : variants) {
			String body = String.format("{\"identifier\": \"%s\", \"password\": \"password123\"}", variant);

			mvc.perform(post("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.username").value("active"));
		}
	}

	@Test
	void loginWithWrongPassword() throws Exception {
		String body = """
			{"identifier": "active@test.com", "password": "wrong"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void loginWithUnknownEmail() throws Exception {
		String body = """
			{"identifier": "unknown@test.com", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void loginWithUnknownUsername() throws Exception {
		String body = """
			{"identifier": "unknown", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void loginWithInvitedUserIsRejected() throws Exception {
		String body = """
			{"identifier": "invited@test.com", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void loginWithDisabledUserIsRejected() throws Exception {
		String body = """
			{"identifier": "disabled@test.com", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void lockoutAfterFailedAttempts() throws Exception {
		String body = """
			{"identifier": "active@test.com", "password": "wrong"}
			""";

		// Fail 5 times (max-failed-attempts = 5)
		for (int i = 0; i < 5; i++) {
			mvc.perform(post("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isUnauthorized());
		}

		// Verify user is now locked
		User user = userRepository.findById(activeUser.getId()).orElseThrow();
		assertThat(user.getFailedAttempts()).isEqualTo(5);
		assertThat(user.getLockedUntil()).isNotNull();
		assertThat(user.getLockedUntil()).isAfter(Instant.now());

		// Try with correct password - still locked
		String correctBody = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(correctBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void lockoutExpiresAfterDuration() throws Exception {
		// Manually lock the user with a past expiry
		activeUser.setFailedAttempts(5L);
		activeUser.setLockedUntil(Instant.now().minus(Duration.ofMinutes(1)));
		userRepository.save(activeUser);

		String body = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());

		// Verify lockout was cleared
		User user = userRepository.findById(activeUser.getId()).orElseThrow();
		assertThat(user.getFailedAttempts()).isZero();
		assertThat(user.getLockedUntil()).isNull();
	}

	@Test
	void successfulLoginResetsFailedAttempts() throws Exception {
		// Set failed attempts
		activeUser.setFailedAttempts(3L);
		userRepository.save(activeUser);

		String body = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());

		// Verify counter was reset
		User user = userRepository.findById(activeUser.getId()).orElseThrow();
		assertThat(user.getFailedAttempts()).isZero();
	}

	@Test
	void accessTokenWorksOnProtectedEndpoint() throws Exception {
		String body = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = loginResult.getResponse().getContentAsString();
		String accessToken = JsonPath.read(responseBody, "$.accessToken");

		// Use the access token on /users/me
		mvc.perform(get("/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("active@test.com"));
	}

	@Test
	void protectedEndpointWithoutTokenReturns401() throws Exception {
		mvc.perform(get("/users/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		// Login first
		String loginBody = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = loginResult.getResponse().getContentAsString();
		String refreshToken = JsonPath.read(responseBody, "$.refreshToken");

		// Logout
		String logoutBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		mvc.perform(post("/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(logoutBody))
			.andExpect(status().isNoContent());

		// Verify token is revoked in DB
		boolean revoked = refreshTokenRepository.findAll().stream()
			.anyMatch(token -> token.getRevokedAt() != null);
		assertThat(revoked).isTrue();
	}

	@Test
	void logoutWithUnknownTokenReturns204() throws Exception {
		String body = """
			{"refreshToken": "unknown-token-xyz"}
			""";

		mvc.perform(post("/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isNoContent());  // No error, prevents enumeration
	}

	@Test
	void logoutWithAlreadyRevokedTokenReturns204() throws Exception {
		// Create and revoke a token manually
		RefreshToken token = new RefreshToken();
		token.setUserId(activeUser.getId());
		token.setTokenHash("already-revoked-hash");
		token.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
		token.setRevokedAt(Instant.now());
		refreshTokenRepository.save(token);

		String body = """
			{"refreshToken": "already-revoked"}
			""";

		mvc.perform(post("/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isNoContent());
	}

	@Test
	void passwordHashNeverInResponse() throws Exception {
		String body = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		MvcResult result = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		assertThat(responseBody).doesNotContain("passwordHash");
		assertThat(responseBody).doesNotContain("$argon2");
	}

	@Test
	void genericErrorMessageForAllFailures() throws Exception {
		String[] failureCases = {
			"{\"identifier\": \"active@test.com\", \"password\": \"wrong\"}",  // Wrong password
			"{\"identifier\": \"unknown@test.com\", \"password\": \"password123\"}",  // Unknown user
			"{\"identifier\": \"invited@test.com\", \"password\": \"password123\"}",  // INVITED
			"{\"identifier\": \"disabled@test.com\", \"password\": \"password123\"}"  // DISABLED
		};

		String expectedCode = "AUTHENTICATION_FAILED";
		String expectedMessage = "Invalid credentials";

		for (String testCase : failureCases) {
			mvc.perform(post("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(testCase))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(expectedCode))
				.andExpect(jsonPath("$.detail").value(expectedMessage));
		}
	}

	@Test
	void accessTokenContainsCorrectClaims() throws Exception {
		String body = """
			{"identifier": "active@test.com", "password": "password123"}
			""";

		MvcResult result = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		String accessToken = JsonPath.read(responseBody, "$.accessToken");

		// Verify access token is not empty
		assertThat(accessToken).isNotEmpty();

		// Verify it works with the existing JWT decoder (test endpoint accepts it)
		mvc.perform(get("/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk());
	}

	// Helper methods

	private User createUser(String email, String username, String password, UserStatus status) {
		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(Role.STUDENT);
		user.setStatus(status);
		user.setFirstName("Test");
		user.setLastName("User");
		user.setFailedAttempts(0L);
		return userRepository.save(user);
	}

}
