package ch.hslu.blebooking.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to refresh an access token using a refresh token.
 */
public record RefreshTokenRequest(
	@NotBlank(message = "Refresh token is required")
	String refreshToken
) {
}
