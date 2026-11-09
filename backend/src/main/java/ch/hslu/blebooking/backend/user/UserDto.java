package ch.hslu.blebooking.backend.user;

/**
 * User information for API responses (login, /users/me, etc.).
 * Does NOT include password hash or other sensitive data.
 */
public record UserDto(
	Long id,
	String email,
	String username,
	String firstName,
	String lastName,
	Role role
) {
}
