package ch.hslu.blebooking.backend.auth.internal;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the table refresh_token. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id")
	private Long userId;
	@Column(name = "token_hash")
	private String tokenHash;
	@Column(name = "expires_at")
	private Instant expiresAt;
	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected RefreshToken() {
		// for JPA
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

}
