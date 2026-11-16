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
 * Creates dev users (ADMIN, STUDENT, STAFF) for manual testing. Each one only if missing.
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
			log.warn("DEV_USER_PASSWORD not set, skipping dev users creation");
			return;
		}

		String hash = passwordEncoder.encode(devPassword);
		createIfMissing("dev@example.com", "dev", hash, Role.ADMIN, "Dev");
		createIfMissing("student@example.com", "student", hash, Role.STUDENT, "Student");
		createIfMissing("staff@example.com", "staff", hash, Role.STAFF, "Staff");
	}

	private void createIfMissing(String email, String username, String passwordHash, Role role, String firstName) {
		if (userManagementService.userExists(email, username)) {
			log.info("Dev user already exists: {}", username);
			return;
		}
		userManagementService.createUser(email, username, passwordHash, role, UserStatus.ACTIVE, firstName, "User");
		log.info("Created dev user: {} / {} ({}, ACTIVE)", email, username, role);
	}

}
