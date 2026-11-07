package ch.hslu.blebooking.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Logout request with refresh token to revoke.
 */
public record LogoutRequest(
	@NotBlank(message = "Refresh token is required")
	String refreshToken
) {
}
