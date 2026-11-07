package ch.hslu.blebooking.backend.auth.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import ch.hslu.blebooking.backend.user.Role;

/**
 * Creates and validates authentication tokens (JWT access tokens and refresh tokens).
 * Designed to be reused by mobile token lifecycle (T-034).
 */
@Service
class TokenService {

	private final String secret;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	TokenService(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl,
			@Value("${app.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
		this.secret = secret;
		this.accessTokenTtl = accessTokenTtl;
		this.refreshTokenTtl = refreshTokenTtl;
	}

	/**
	 * Create a JWT access token (HS256) with userId and role.
	 * @param userId the user ID (becomes the "sub" claim)
	 * @param role the user's role
	 * @return signed JWT string
	 */
	String createAccessToken(Long userId, Role role) {
		try {
			Instant now = Instant.now();
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(String.valueOf(userId))
				.claim("role", role.name())
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plus(accessTokenTtl)))
				.build();

			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
			return jwt.serialize();
		}
		catch (JOSEException e) {
			throw new IllegalStateException("Failed to sign JWT", e);
		}
	}

	/**
	 * Create a refresh token: 32-byte random value (Base64URL encoded).
	 * Returns both the raw token (to send to the client) and the entity with its SHA-256 hash.
	 * @param userId the user ID
	 * @return refresh token data (raw + entity)
	 */
	RefreshTokenData createRefreshToken(Long userId) {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

		RefreshToken entity = new RefreshToken();
		entity.setUserId(userId);
		entity.setTokenHash(hashRefreshToken(rawToken));
		entity.setExpiresAt(Instant.now().plus(refreshTokenTtl));

		return new RefreshTokenData(rawToken, entity);
	}

	/**
	 * Hash a refresh token with SHA-256 for storage.
	 * Used during logout to hash the received token before lookup.
	 * SHA-256 is secure for random tokens and allows efficient database lookups.
	 * @param rawToken the raw refresh token
	 * @return hex-encoded SHA-256 hash
	 */
	String hashRefreshToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * Holds the raw refresh token (sent to client) and the entity (for DB storage).
	 */
	record RefreshTokenData(String rawToken, RefreshToken entity) {
	}

}
