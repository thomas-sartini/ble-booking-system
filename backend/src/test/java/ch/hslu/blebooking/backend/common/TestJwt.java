package ch.hslu.blebooking.backend.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Creates access tokens for tests, signed like the real ones (HS256).
 */
public final class TestJwt {

	private TestJwt() {
	}

	public static String token(String secret, String role) {
		return sign(secret, role, "1", Instant.now().plusSeconds(300));
	}

	/** Token with a given "sub" claim, e.g. the id of a real user. */
	public static String token(String secret, String role, String subject) {
		return sign(secret, role, subject, Instant.now().plusSeconds(300));
	}

	public static String expiredToken(String secret, String role) {
		return sign(secret, role, "1", Instant.now().minusSeconds(60));
	}

	private static String sign(String secret, String role, String subject, Instant expiresAt) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(subject)
				.claim("role", role)
				.issueTime(new Date())
				.expirationTime(Date.from(expiresAt))
				.build();
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
			return jwt.serialize();
		}
		catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

}
