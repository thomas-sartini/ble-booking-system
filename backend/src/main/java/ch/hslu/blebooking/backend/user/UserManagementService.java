package ch.hslu.blebooking.backend.user;

import java.util.Optional;

/**
 * Public API for user management operations.
 * Allows other modules to create and manage users without accessing user.internal classes.
 */
public interface UserManagementService {

	/**
	 * Create a new user. Only for administrative/dev purposes.
	 * @param email user email
	 * @param username username
	 * @param passwordHash hashed password (Argon2)
	 * @param role user role
	 * @param status user status
	 * @param firstName first name
	 * @param lastName last name
	 */
	void createUser(String email, String username, String passwordHash, Role role, UserStatus status,
			String firstName, String lastName);

	/**
	 * Check if a user with the given email or username already exists.
	 * @param email email to check
	 * @param username username to check
	 * @return true if user exists
	 */
	boolean userExists(String email, String username);

	/**
	 * Get user details by ID.
	 * @param userId user ID
	 * @return user DTO if found, empty otherwise
	 */
	Optional<UserDto> getUserById(Long userId);

}
