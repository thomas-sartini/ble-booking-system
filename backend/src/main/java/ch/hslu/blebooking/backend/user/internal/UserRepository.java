package ch.hslu.blebooking.backend.user.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * Find user by email or username (case-insensitive).
	 * Spring Data JPA generates the query automatically.
	 */
	Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

}
