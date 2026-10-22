package ch.hslu.blebooking.backend.common.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.hslu.blebooking.backend.TestcontainersConfiguration;
import ch.hslu.blebooking.backend.common.TestEndpoints;
import ch.hslu.blebooking.backend.common.TestJwt;

/**
 * Checks the security baseline: access token required, /auth/** open, roles from the token.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, TestEndpoints.class })
class SecurityIT {

	@Autowired
	private WebApplicationContext context;

	@Value("${app.jwt.secret}")
	private String secret;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void requestWithoutTokenIsRejected() throws Exception {
		mvc.perform(get("/test/authenticated"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void authEndpointsDoNotNeedToken() throws Exception {
		// no endpoint exists yet, so 404 instead of 401 shows that security let the request through
		mvc.perform(get("/auth/does-not-exist"))
			.andExpect(status().isNotFound());
	}

	@Test
	void validTokenIsAccepted() throws Exception {
		mvc.perform(get("/test/authenticated").header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.token(secret, "STUDENT"))))
			.andExpect(status().isOk());
	}

	@Test
	void expiredTokenIsRejected() throws Exception {
		mvc.perform(get("/test/authenticated").header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.expiredToken(secret, "STUDENT"))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenWithWrongSignatureIsRejected() throws Exception {
		String forged = TestJwt.token("another-secret-that-is-also-32-bytes-long", "ADMIN");
		mvc.perform(get("/test/authenticated").header(HttpHeaders.AUTHORIZATION, bearer(forged)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongRoleIsForbidden() throws Exception {
		mvc.perform(get("/test/admin").header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.token(secret, "STUDENT"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void roleFromTokenIsUsed() throws Exception {
		mvc.perform(get("/test/admin").header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.token(secret, "ADMIN"))))
			.andExpect(status().isOk());
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

}
