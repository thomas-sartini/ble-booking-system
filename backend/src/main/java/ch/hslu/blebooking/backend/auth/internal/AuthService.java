package ch.hslu.blebooking.backend.auth.internal;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.auth.TokenRevocationService;
import ch.hslu.blebooking.backend.auth.dto.LoginResponse;
import ch.hslu.blebooking.backend.auth.dto.RefreshTokenResponse;
import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;
import ch.hslu.blebooking.backend.user.AuthUserDetails;
import ch.hslu.blebooking.backend.user.UserAuthService;
import ch.hslu.blebooking.backend.user.UserDto;
import ch.hslu.blebooking.backend.user.UserStatus;

/**
 * Handles login, logout and token refresh with security best practices:
 * - Generic error messages (no user enumeration)
 * - Timing-safe password verification
 * - Account lockout after failed attempts
 * - Refresh token management with rotation and reuse detection
 */
@Service
@Transactional
public class AuthService implements TokenRevocationService {

	private static final String GENERIC_AUTH_ERROR = "Invalid credentials";

	private final UserAuthService userAuthService;
	private final TokenService tokenService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final long accessTokenTtlSeconds;

	// Timing safety: valid Argon2 hash created at startup to prevent timing attacks when user doesn't exist
	private final String dummyPasswordHash;

	public AuthService(UserAuthService userAuthService,
			TokenService tokenService,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl) {
		this.userAuthService = userAuthService;
		this.tokenService = tokenService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenTtlSeconds = accessTokenTtl.toSeconds();
		// Generate a valid dummy hash once at startup for timing-safe operations
		this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-safety");
	}

	/**
	 * Authenticate a user by email or username.
	 * Returns access token, refresh token and user details on success.
	 * Throws AUTHENTICATION_FAILED for any failure (wrong password, unknown user, disabled account, lockout).
	 *
	 * Uses noRollbackFor to ensure failed login attempts are persisted even when throwing ApiException.
	 * This is critical for account lockout to work in production.
	 *
	 * @param identifier email or username (case-insensitive)
	 * @param password plaintext password
	 * @return login response with tokens and user data
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public LoginResponse login(String identifier, String password) {
		AuthUserDetails user = userAuthService.findByIdentifier(identifier).orElse(null);

		// Timing-safe: always hash a password even if user doesn't exist or has no password (INVITED)
		String passwordHash = (user != null && user.passwordHash() != null) ? user.passwordHash() : dummyPasswordHash;
		boolean passwordMatches = passwordEncoder.matches(password, passwordHash);

		// Check all failure conditions with the same error
		if (user == null) {
			// Unknown user - don't record failed attempt, just return generic error
			throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, GENERIC_AUTH_ERROR);
		}

		if (!passwordMatches) {
			// Wrong password - record failed attempt
			userAuthService.recordFailedAttempt(user.id());
			throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, GENERIC_AUTH_ERROR);
		}

		// Account locked
		if (user.lockedUntil() != null && user.lockedUntil().isAfter(Instant.now())) {
			throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, GENERIC_AUTH_ERROR);
		}

		// Only ACTIVE users may log in
		if (user.status() != UserStatus.ACTIVE) {
			throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, GENERIC_AUTH_ERROR);
		}

		// Success: create tokens
		userAuthService.recordSuccessfulLogin(user.id());

		String accessToken = tokenService.createAccessToken(user.id(), user.role());
		TokenService.RefreshTokenData refreshData = tokenService.createRefreshToken(user.id());
		refreshTokenRepository.save(refreshData.entity());

		UserDto userDto = new UserDto(
			user.id(),
			user.email(),
			user.username(),
			user.firstName(),
			user.lastName(),
			user.role()
		);

		return new LoginResponse(
			accessToken,
			"Bearer",
			accessTokenTtlSeconds,
			refreshData.rawToken(),
			userDto
		);
	}

	/**
	 * Revoke a refresh token. Always succeeds (returns normally) even if the token doesn't exist,
	 * to prevent token enumeration attacks.
	 *
	 * @param rawRefreshToken the refresh token to revoke
	 */
	public void logout(String rawRefreshToken) {
		String hash = tokenService.hashRefreshToken(rawRefreshToken);
		refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
			token.setRevokedAt(Instant.now());
		});
		// No error if token not found - prevents information leakage
	}

	/**
	 * Refresh an access token using a refresh token.
	 * Implements token rotation: the presented refresh token is revoked and a new one is issued.
	 * Implements reuse detection: if a revoked token is presented, all user tokens are revoked.
	 *
	 * Uses noRollbackFor to ensure token revocations (including reuse detection) are persisted
	 * even when throwing ApiException. This is critical for security.
	 *
	 * @param rawRefreshToken the refresh token
	 * @return new access token and new refresh token
	 * @throws ApiException with INVALID_REFRESH_TOKEN for any failure (generic error)
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public RefreshTokenResponse refreshAccessToken(String rawRefreshToken) {
		String hash = tokenService.hashRefreshToken(rawRefreshToken);

		// Lock the token row to prevent concurrent refresh with the same token
		RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(hash)
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token"));

		// Check if token was already revoked (possible reuse attack)
		if (token.getRevokedAt() != null) {
			// Reuse detection: revoke all active tokens for this user
			revokeAllForUser(token.getUserId());
			throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token");
		}

		// Check expiry
		if (token.getExpiresAt().isBefore(Instant.now())) {
			throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token");
		}

		// Verify user is still ACTIVE
		AuthUserDetails user = userAuthService.findById(token.getUserId())
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token"));

		if (user.status() != UserStatus.ACTIVE) {
			throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token");
		}

		// Rotation: revoke the presented token
		token.setRevokedAt(Instant.now());

		// Create new tokens
		String newAccessToken = tokenService.createAccessToken(token.getUserId(), user.role());
		TokenService.RefreshTokenData newRefreshData = tokenService.createRefreshToken(token.getUserId());
		refreshTokenRepository.save(newRefreshData.entity());

		return new RefreshTokenResponse(
			newAccessToken,
			"Bearer",
			accessTokenTtlSeconds,
			newRefreshData.rawToken()
		);
	}

	/**
	 * Revoke all active refresh tokens for a user.
	 * Implements TokenRevocationService for use by other modules.
	 *
	 * @param userId the user ID
	 */
	@Override
	public void revokeAllForUser(Long userId) {
		refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
	}

}
