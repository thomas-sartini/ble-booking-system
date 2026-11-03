package ch.hslu.blebooking.backend.user.internal;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.user.Role;
import ch.hslu.blebooking.backend.user.UserDto;
import ch.hslu.blebooking.backend.user.UserManagementService;
import ch.hslu.blebooking.backend.user.UserStatus;

@Service
@Transactional
class UserManagementServiceImpl implements UserManagementService {

	private final UserRepository userRepository;

	UserManagementServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public void createUser(String email, String username, String passwordHash, Role role, UserStatus status,
			String firstName, String lastName) {
		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setPasswordHash(passwordHash);
		user.setRole(role);
		user.setStatus(status);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setFailedAttempts(0L);
		userRepository.save(user);
	}

	@Override
	public boolean userExists(String email, String username) {
		return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(email, username).isPresent();
	}

	@Override
	public Optional<UserDto> getUserById(Long userId) {
		return userRepository.findById(userId)
			.map(user -> new UserDto(
				user.getId(),
				user.getEmail(),
				user.getUsername(),
				user.getFirstName(),
				user.getLastName(),
				user.getRole()
			));
	}

}
