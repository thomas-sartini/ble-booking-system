package ch.hslu.blebooking.backend.auth.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.hslu.blebooking.backend.user.Role;
import ch.hslu.blebooking.backend.user.UserManagementService;
import ch.hslu.blebooking.backend.user.UserStatus;

/**
 * Creates a dev user for manual testing.
 * Only active in the "dev" profile, never in production or test.
 */
@Component
@Profile("dev")
class DevUserInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevUserInitializer.class);

	private final UserManagementService userManagementService;
	private final PasswordEncoder passwordEncoder;

	@Value("${DEV_USER_PASSWORD:#{null}}")
	private String devPassword;

	DevUserInitializer(UserManagementService userManagementService, PasswordEncoder passwordEncoder) {
		this.userManagementService = userManagementService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (devPassword == null || devPassword.isBlank()) {
			log.warn("DEV_USER_PASSWORD not set, skipping dev user creation");
			return;
		}

		// Check if dev user already exists
		if (userManagementService.userExists("dev@example.com", "dev")) {
			log.info("Dev user already exists");
			return;
		}

		// Create dev user
		userManagementService.createUser(
			"dev@example.com",
			"dev",
			passwordEncoder.encode(devPassword),
			Role.ADMIN,
			UserStatus.ACTIVE,
			"Dev",
			"User"
		);

		log.info("Created dev user: dev@example.com / dev (ADMIN, ACTIVE)");
	}

}
