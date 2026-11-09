package ch.hslu.blebooking.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request with email or username.
 */
public record LoginRequest(
	@NotBlank(message = "Identifier is required")
	String identifier,  // email OR username

	@NotBlank(message = "Password is required")
	String password
) {
}
