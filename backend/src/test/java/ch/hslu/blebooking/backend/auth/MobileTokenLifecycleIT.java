package ch.hslu.blebooking.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import ch.hslu.blebooking.backend.user.Role;
import ch.hslu.blebooking.backend.user.UserStatus;
import ch.hslu.blebooking.backend.user.internal.User;
import ch.hslu.blebooking.backend.user.internal.UserRepository;

/**
 * Integration tests for mobile token lifecycle (T-034 & T-035).
 * Tests token refresh, rotation, reuse detection and revocation.
 * Covers FR-01.06, FR-02.02, NFR-01.02.
 *
 * Note: @Transactional is NOT used to ensure tests verify real database commits.
 * This is critical for testing token revocation and reuse detection.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MobileTokenLifecycleIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenRevocationService tokenRevocationService;

	private MockMvc mvc;

	private User activeUser;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

		// Clean up any existing test data
		cleanupTestData();

		activeUser = createUser("mobile@test.com", "mobileuser", "password123", UserStatus.ACTIVE);
	}

	@org.junit.jupiter.api.AfterEach
	void tearDown() {
		// Clean up test data after each test to ensure isolation
		cleanupTestData();
	}

	private void cleanupTestData() {
		// Delete refresh tokens first (foreign key to user)
		refreshTokenRepository.deleteAll();
		// Then delete users
		userRepository.deleteAll();
	}

	@Test
	void refreshWithValidTokenReturnsNewTokensAndRevokesOld() throws Exception {
		// Login to get initial tokens
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String loginResponse = loginResult.getResponse().getContentAsString();
		String oldRefreshToken = JsonPath.read(loginResponse, "$.refreshToken");
		String oldAccessToken = JsonPath.read(loginResponse, "$.accessToken");

		// Refresh the token
		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", oldRefreshToken);

		MvcResult refreshResult = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.refreshToken").isString())
			.andExpect(jsonPath("$.user").doesNotExist())  // No user object in refresh response
			.andReturn();

		String refreshResponse = refreshResult.getResponse().getContentAsString();
		String newAccessToken = JsonPath.read(refreshResponse, "$.accessToken");
		String newRefreshToken = JsonPath.read(refreshResponse, "$.refreshToken");

		// Verify refresh tokens are different
		assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
		// Note: access tokens might be the same if created within the same second (JWT has 1s granularity)

		// Verify old refresh token is now revoked in database
		List<RefreshToken> tokens = refreshTokenRepository.findAll();
		boolean oldTokenRevoked = tokens.stream()
			.anyMatch(t -> t.getRevokedAt() != null);
		assertThat(oldTokenRevoked).isTrue();
	}

	@Test
	void newAccessTokenWorksOnProtectedEndpoint() throws Exception {
		// Login
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String loginResponse = loginResult.getResponse().getContentAsString();
		String refreshToken = JsonPath.read(loginResponse, "$.refreshToken");

		// Refresh
		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		MvcResult refreshResult = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isOk())
			.andReturn();

		String refreshResponse = refreshResult.getResponse().getContentAsString();
		String newAccessToken = JsonPath.read(refreshResponse, "$.accessToken");

		// Use new access token on protected endpoint
		mvc.perform(get("/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("mobile@test.com"));
	}

	@Test
	void oldRefreshTokenRejectedAfterRotation() throws Exception {
		// Login
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String loginResponse = loginResult.getResponse().getContentAsString();
		String oldRefreshToken = JsonPath.read(loginResponse, "$.refreshToken");

		// Refresh successfully
		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", oldRefreshToken);

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isOk());

		// Try to use old refresh token again (should fail)
		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
			.andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
	}

	@Test
	void reuseDetectionRevokesAllUserTokens() throws Exception {
		// Login to get initial token
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult login = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String token1 = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

		// Refresh token1 (this revokes it and creates token2)
		String refreshBody1 = String.format("{\"refreshToken\": \"%s\"}", token1);

		MvcResult refresh1 = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody1))
			.andExpect(status().isOk())
			.andReturn();

		String token2 = JsonPath.read(refresh1.getResponse().getContentAsString(), "$.refreshToken");

		// Refresh token2 (this revokes it and creates token3)
		String refreshBody2 = String.format("{\"refreshToken\": \"%s\"}", token2);

		MvcResult refresh2 = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody2))
			.andExpect(status().isOk())
			.andReturn();

		String token3 = JsonPath.read(refresh2.getResponse().getContentAsString(), "$.refreshToken");

		// Verify we have 1 active token before reuse detection
		List<RefreshToken> activeBefore = refreshTokenRepository.findAllActiveByUserId(activeUser.getId());
		assertThat(activeBefore).hasSize(1);  // only token3

		// Try to reuse token2 (already revoked) - this should revoke ALL tokens
		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody2))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		// Verify ALL tokens for this user are now revoked in the database
		// This proves the revocation was committed despite the ApiException
		List<RefreshToken> activeAfter = refreshTokenRepository.findAllActiveByUserId(activeUser.getId());
		assertThat(activeAfter).as("All tokens should be revoked after reuse detection").isEmpty();

		// Also verify all tokens have revokedAt set
		List<RefreshToken> allTokens = refreshTokenRepository.findAll().stream()
			.filter(t -> t.getUserId().equals(activeUser.getId()))
			.toList();
		assertThat(allTokens).as("All user tokens should have revokedAt set")
			.allMatch(t -> t.getRevokedAt() != null);
	}

	@Test
	void expiredTokenRejected() throws Exception {
		// Create an expired refresh token manually
		RefreshToken expiredToken = new RefreshToken();
		expiredToken.setUserId(activeUser.getId());
		expiredToken.setTokenHash("expired-token-hash-for-test");
		expiredToken.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));
		refreshTokenRepository.save(expiredToken);

		String refreshBody = """
			{"refreshToken": "expired-token-raw"}
			""";

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
			.andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
	}

	@Test
	void revokedTokenRejected() throws Exception {
		// Login
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");

		// Logout (revokes the token)
		String logoutBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		mvc.perform(post("/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(logoutBody))
			.andExpect(status().isNoContent());

		// Try to refresh with revoked token
		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void unknownTokenRejected() throws Exception {
		String refreshBody = """
			{"refreshToken": "completely-unknown-token-xyz"}
			""";

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
			.andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
	}

	@Test
	void disabledUserRefreshRejected() throws Exception {
		// Login as active user
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");

		// Disable the user
		activeUser.setStatus(UserStatus.DISABLED);
		userRepository.save(activeUser);

		// Try to refresh - should fail
		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
			.andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
	}

	@Test
	void revokeAllForUserInvalidatesOnlyThatUser() throws Exception {
		// Create second user
		User otherUser = createUser("other@test.com", "otheruser", "password123", UserStatus.ACTIVE);

		// Login both users
		String loginBody1 = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		String loginBody2 = """
			{"identifier": "other@test.com", "password": "password123"}
			""";

		MvcResult login1 = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody1))
			.andExpect(status().isOk())
			.andReturn();

		MvcResult login2 = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody2))
			.andExpect(status().isOk())
			.andReturn();

		String token1 = JsonPath.read(login1.getResponse().getContentAsString(), "$.refreshToken");
		String token2 = JsonPath.read(login2.getResponse().getContentAsString(), "$.refreshToken");

		// Revoke all tokens for activeUser
		tokenRevocationService.revokeAllForUser(activeUser.getId());

		// Verify database state
		List<RefreshToken> activeUser1Tokens = refreshTokenRepository.findAllActiveByUserId(activeUser.getId());
		List<RefreshToken> otherUserTokens = refreshTokenRepository.findAllActiveByUserId(otherUser.getId());

		assertThat(activeUser1Tokens).as("ActiveUser tokens should all be revoked").isEmpty();
		assertThat(otherUserTokens).as("OtherUser should still have 1 active token").hasSize(1);

		// Also check all tokens for activeUser (including revoked)
		List<RefreshToken> allActiveUserTokens = refreshTokenRepository.findAll().stream()
			.filter(t -> t.getUserId().equals(activeUser.getId()))
			.toList();
		assertThat(allActiveUserTokens).as("All activeUser tokens should have revokedAt set")
			.allMatch(t -> t.getRevokedAt() != null);

		// activeUser's token should be rejected
		String refreshBody1 = String.format("{\"refreshToken\": \"%s\"}", token1);

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody1))
			.andExpect(status().isUnauthorized());

		// otherUser's token should still work
		String refreshBody2 = String.format("{\"refreshToken\": \"%s\"}", token2);

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody2))
			.andExpect(status().isOk());
	}

	@Test
	void concurrentRefreshWithSameTokenOnlyOneSucceeds() throws Exception {
		// Login
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");

		// Try to refresh twice concurrently with the same token
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		Runnable refreshTask = () -> {
			try {
				MockMvc localMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
				readyLatch.countDown();  // Signal this thread is ready
				startLatch.await();  // Wait for start signal

				MvcResult result = localMvc.perform(post("/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody))
					.andReturn();

				if (result.getResponse().getStatus() == 200) {
					successCount.incrementAndGet();
				}
				else {
					failureCount.incrementAndGet();
				}
			}
			catch (Exception e) {
				failureCount.incrementAndGet();
			}
		};

		executor.submit(refreshTask);
		executor.submit(refreshTask);

		readyLatch.await();  // Wait for both threads to be ready
		startLatch.countDown();  // Start both threads at the same time
		executor.shutdown();

		// Wait for completion (up to 5 seconds)
		boolean finished = executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
		assertThat(finished).as("Both threads should complete within 5 seconds").isTrue();

		// Exactly one should succeed due to pessimistic locking (SELECT ... FOR UPDATE)
		assertThat(successCount.get()).as("Exactly one refresh should succeed").isEqualTo(1);
		assertThat(failureCount.get()).as("Exactly one refresh should fail").isEqualTo(1);
	}

	@Test
	void fullChainLoginRefreshRefreshLogoutRefreshFails() throws Exception {
		// 1. Login
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult login = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String token1 = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

		// 2. First refresh
		String refreshBody1 = String.format("{\"refreshToken\": \"%s\"}", token1);

		MvcResult refresh1 = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody1))
			.andExpect(status().isOk())
			.andReturn();

		String token2 = JsonPath.read(refresh1.getResponse().getContentAsString(), "$.refreshToken");

		// 3. Second refresh
		String refreshBody2 = String.format("{\"refreshToken\": \"%s\"}", token2);

		MvcResult refresh2 = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody2))
			.andExpect(status().isOk())
			.andReturn();

		String token3 = JsonPath.read(refresh2.getResponse().getContentAsString(), "$.refreshToken");

		// 4. Logout
		String logoutBody = String.format("{\"refreshToken\": \"%s\"}", token3);

		mvc.perform(post("/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(logoutBody))
			.andExpect(status().isNoContent());

		// 5. Try to refresh after logout - should fail
		String refreshBody3 = String.format("{\"refreshToken\": \"%s\"}", token3);

		mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody3))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void refreshResponseNeverContainsHash() throws Exception {
		// Login
		String loginBody = """
			{"identifier": "mobile@test.com", "password": "password123"}
			""";

		MvcResult loginResult = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andReturn();

		String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");

		// Refresh
		String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

		MvcResult refreshResult = mvc.perform(post("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody))
			.andExpect(status().isOk())
			.andReturn();

		String response = refreshResult.getResponse().getContentAsString();
		assertThat(response).doesNotContain("tokenHash");
		assertThat(response).doesNotContain("hash");
	}

	@Test
	void errorResponsesAreGeneric() throws Exception {
		String[] failureCases = {
			"{\"refreshToken\": \"unknown-token\"}",  // Unknown
			"{\"refreshToken\": \"expired-token\"}",  // Expired (if it existed)
		};

		String expectedCode = "INVALID_REFRESH_TOKEN";
		String expectedMessage = "Invalid or expired refresh token";

		for (String testCase : failureCases) {
			mvc.perform(post("/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(testCase))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(expectedCode))
				.andExpect(jsonPath("$.detail").value(expectedMessage));
		}
	}

	// Helper method

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
