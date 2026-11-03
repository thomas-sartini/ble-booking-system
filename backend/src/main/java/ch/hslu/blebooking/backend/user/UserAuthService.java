package ch.hslu.blebooking.backend.user;

import java.util.Optional;

/**
 * Public API for the auth module to perform user lookups and track login attempts
 * without accessing user.internal classes directly.
 */
public interface UserAuthService {

	/**
	 * Find a user by email or username (case-insensitive).
	 * @param identifier email or username
	 * @return user details if found
	 */
	Optional<AuthUserDetails> findByIdentifier(String identifier);

	/**
	 * Record a failed login attempt. Increments failed_attempts and locks the account
	 * if the threshold is exceeded.
	 * @param userId the user ID
	 */
	void recordFailedAttempt(Long userId);

	/**
	 * Record a successful login. Resets failed_attempts to 0 and clears locked_until.
	 * @param userId the user ID
	 */
	void recordSuccessfulLogin(Long userId);

}
