package ch.hslu.blebooking.backend.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;

/**
 * User endpoints. Requires authentication.
 */
@RestController
@RequestMapping("/users")
class UserController {

	private final UserManagementService userManagementService;

	UserController(UserManagementService userManagementService) {
		this.userManagementService = userManagementService;
	}

	/**
	 * Get the currently authenticated user's details.
	 * The user ID comes from the JWT "sub" claim.
	 */
	@GetMapping("/me")
	UserDto getCurrentUser(Authentication auth) {
		Long userId = Long.parseLong(auth.getName());  // "sub" claim from JWT
		return userManagementService.getUserById(userId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
	}

}
