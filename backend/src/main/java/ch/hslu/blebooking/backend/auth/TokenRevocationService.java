package ch.hslu.blebooking.backend.auth;

/**
 * Public API for revoking user authentication sessions.
 * Intended for use by other modules when user credentials are invalidated
 * (e.g., password change, account deactivation).
 *
 * Implemented by AuthService and exposed as a public API for future integration
 * with account management features (FR-01.03).
 */
public interface TokenRevocationService {

	/**
	 * Revoke all active refresh tokens for a user.
	 * This invalidates all renewable sessions, forcing re-authentication.
	 * Access tokens remain valid until their expiry (max 15 minutes).
	 *
	 * @param userId the user ID whose tokens should be revoked
	 */
	void revokeAllForUser(Long userId);

}
