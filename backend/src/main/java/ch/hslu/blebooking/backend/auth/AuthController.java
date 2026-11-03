package ch.hslu.blebooking.backend.auth;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.hslu.blebooking.backend.auth.dto.LoginRequest;
import ch.hslu.blebooking.backend.auth.dto.LoginResponse;
import ch.hslu.blebooking.backend.auth.dto.LogoutRequest;
import ch.hslu.blebooking.backend.auth.dto.RefreshTokenRequest;
import ch.hslu.blebooking.backend.auth.dto.RefreshTokenResponse;
import ch.hslu.blebooking.backend.auth.internal.AuthService;
import jakarta.validation.Valid;

/**
 * Authentication endpoints for web and mobile login/logout/refresh.
 * No authentication required (endpoints under /auth/** are permitted by SecurityConfig).
 */
@RestController
@RequestMapping("/auth")
class AuthController {

	private final AuthService authService;

	AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * Login with email or username.
	 * Returns access token, refresh token and user details.
	 * Returns 401 AUTHENTICATION_FAILED for any failure (generic error, no hints).
	 */
	@PostMapping("/login")
	LoginResponse login(@RequestBody @Valid LoginRequest request) {
		return authService.login(request.identifier(), request.password());
	}

	/**
	 * Refresh an access token using a refresh token (mobile and web).
	 * Implements token rotation: returns new access token and new refresh token.
	 * The old refresh token is revoked.
	 * Returns 401 INVALID_REFRESH_TOKEN for any failure (expired, revoked, invalid).
	 */
	@PostMapping("/refresh")
	RefreshTokenResponse refresh(@RequestBody @Valid RefreshTokenRequest request) {
		return authService.refreshAccessToken(request.refreshToken());
	}

	/**
	 * Logout by revoking a refresh token.
	 * Always returns 204 (even if token doesn't exist) to prevent information leakage.
	 */
	@PostMapping("/logout")
	@ResponseStatus(NO_CONTENT)
	void logout(@RequestBody @Valid LogoutRequest request) {
		authService.logout(request.refreshToken());
	}

}
