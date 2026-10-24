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
		return sign(secret, role, Instant.now().plusSeconds(300));
	}

	public static String expiredToken(String secret, String role) {
		return sign(secret, role, Instant.now().minusSeconds(60));
	}

	private static String sign(String secret, String role, Instant expiresAt) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject("1")
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
