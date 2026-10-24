package ch.hslu.blebooking.backend.auth.internal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {
}
