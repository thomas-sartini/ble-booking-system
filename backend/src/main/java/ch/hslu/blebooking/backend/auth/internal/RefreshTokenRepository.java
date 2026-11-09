package ch.hslu.blebooking.backend.auth.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Find and lock a refresh token by its hash for atomic refresh operations.
	 * Uses pessimistic write lock (SELECT ... FOR UPDATE) to prevent concurrent refreshes.
	 * @param tokenHash the SHA-256 hash of the refresh token
	 * @return the locked token if found
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

	/**
	 * Find all active (non-revoked) refresh tokens for a user.
	 * Used for reuse detection and bulk revocation.
	 * @param userId the user ID
	 * @return list of active tokens
	 */
	@Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
	List<RefreshToken> findAllActiveByUserId(Long userId);

	/**
	 * Revoke all active refresh tokens for a user.
	 * Used when detecting refresh token reuse (possible theft) or when user credentials are invalidated.
	 * @param userId the user ID
	 * @param revokedAt the revocation timestamp
	 * @return the number of tokens revoked
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
	int revokeAllActiveByUserId(Long userId, Instant revokedAt);

}
