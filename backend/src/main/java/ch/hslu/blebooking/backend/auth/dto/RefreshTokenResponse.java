package ch.hslu.blebooking.backend.auth.dto;

/**
 * Response from token refresh endpoint.
 * Returns new access token and new refresh token (rotation).
 * No user object needed - mobile already knows the user.
 */
public record RefreshTokenResponse(
	String accessToken,
	String tokenType,     // "Bearer"
	Long expiresIn,       // seconds (900 for 15 minutes)
	String refreshToken   // new rotated refresh token
) {
}
