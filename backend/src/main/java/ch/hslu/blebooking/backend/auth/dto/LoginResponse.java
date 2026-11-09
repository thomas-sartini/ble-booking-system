package ch.hslu.blebooking.backend.auth.dto;

import ch.hslu.blebooking.backend.user.UserDto;

/**
 * Login response with access token, refresh token and user details.
 */
public record LoginResponse(
	String accessToken,
	String tokenType,     // "Bearer"
	Long expiresIn,       // seconds (900 for 15 minutes)
	String refreshToken,
	UserDto user
) {
}
