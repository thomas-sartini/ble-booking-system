package ch.hslu.blebooking.backend.user;

import java.time.Instant;

/**
 * User details needed for authentication. Exposes only the minimal data required by the auth module
 * without leaking the full internal User entity.
 */
public record AuthUserDetails(
	Long id,
	String email,
	String username,
	String passwordHash,
	Role role,
	UserStatus status,
	Long failedAttempts,
	Instant lockedUntil
) {
}
