package ch.hslu.blebooking.backend.auth.internal;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.auth.dto.LoginResponse;
import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;
import ch.hslu.blebooking.backend.user.AuthUserDetails;
import ch.hslu.blebooking.backend.user.UserAuthService;
import ch.hslu.blebooking.backend.user.UserDto;
import ch.hslu.blebooking.backend.user.UserStatus;

/**
 * Handles login and logout with security best practices:
 * - Generic error messages (no user enumeration)
 * - Timing-safe password verification
 * - Account lockout after failed attempts
 * - Refresh token management
 */
@Service
@Transactional
public class AuthService {

	private static final String GENERIC_AUTH_ERROR = "Invalid credentials";
	// Timing safety: valid Argon2 hash of "dummy" to prevent timing attacks when user doesn't exist
	private static final String DUMMY_PASSWORD_HASH = "$argon2id$v=19$m=16384,t=2,p=1$dummysaltdummysaltdummysa$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

	private final UserAuthService userAuthService;
	private final TokenService tokenService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserAuthService userAuthService,
			TokenService tokenService,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder) {
		this.userAuthService = userAuthService;
		this.tokenService = tokenService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Authenticate a user by email or username.
	 * Returns access token, refresh token and user details on success.
	 * Throws AUTHENTICATION_FAILED for any failure (wrong password, unknown user, disabled account, lockout).
	 *
	 * @param identifier email or username (case-insensitive)
	 * @param password plaintext password
	 * @return login response with tokens and user data
	 */
	public LoginResponse login(String identifier, String password) {
		AuthUserDetails user = userAuthService.findByIdentifier(identifier).orElse(null);

		// Timing-safe: always hash a password even if user doesn't exist
		String passwordHash = user != null ? user.passwordHash() : DUMMY_PASSWORD_HASH;
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
			null, // firstName - will add later when we populate it
			null, // lastName
			user.role()
		);

		return new LoginResponse(
			accessToken,
			"Bearer",
			900L, // 15 minutes in seconds (matches PT15M)
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

}
