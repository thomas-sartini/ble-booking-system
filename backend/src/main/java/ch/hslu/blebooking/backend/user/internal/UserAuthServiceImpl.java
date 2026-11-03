package ch.hslu.blebooking.backend.user.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.user.AuthUserDetails;
import ch.hslu.blebooking.backend.user.UserAuthService;

/**
 * Implementation of UserAuthService that performs case-insensitive user lookups
 * and manages login attempt tracking.
 */
@Service
@Transactional
class UserAuthServiceImpl implements UserAuthService {

	private final UserRepository userRepository;
	private final int maxFailedAttempts;
	private final Duration lockoutDuration;

	UserAuthServiceImpl(UserRepository userRepository,
			@Value("${app.auth.max-failed-attempts}") int maxFailedAttempts,
			@Value("${app.auth.lockout-duration}") Duration lockoutDuration) {
		this.userRepository = userRepository;
		this.maxFailedAttempts = maxFailedAttempts;
		this.lockoutDuration = lockoutDuration;
	}

	@Override
	public Optional<AuthUserDetails> findByIdentifier(String identifier) {
		return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
			.map(this::toAuthUserDetails);
	}

	@Override
	public void recordFailedAttempt(Long userId) {
		userRepository.findById(userId).ifPresent(user -> {
			long newCount = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
			user.setFailedAttempts(newCount);

			if (newCount >= maxFailedAttempts) {
				user.setLockedUntil(Instant.now().plus(lockoutDuration));
			}
		});
	}

	@Override
	public void recordSuccessfulLogin(Long userId) {
		userRepository.findById(userId).ifPresent(user -> {
			user.setFailedAttempts(0L);
			user.setLockedUntil(null);
		});
	}

	private AuthUserDetails toAuthUserDetails(User user) {
		return new AuthUserDetails(
			user.getId(),
			user.getEmail(),
			user.getUsername(),
			user.getPasswordHash(),
			user.getRole(),
			user.getStatus(),
			user.getFailedAttempts(),
			user.getLockedUntil(),
			user.getFirstName(),
			user.getLastName()
		);
	}

}
