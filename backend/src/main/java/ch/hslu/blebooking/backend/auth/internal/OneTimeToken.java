package ch.hslu.blebooking.backend.auth.internal;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the table one_time_token. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "one_time_token")
public class OneTimeToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id")
	private Long userId;
	@Column(name = "token_hash")
	private String tokenHash;
	@Column(name = "purpose")
	private String purpose;
	@Column(name = "expires_at")
	private Instant expiresAt;
	@Column(name = "used_at")
	private Instant usedAt;
	@Column(name = "created_by_user_id")
	private Long createdByUserId;

	protected OneTimeToken() {
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

	public String getPurpose() {
		return purpose;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public Long getCreatedByUserId() {
		return createdByUserId;
	}

}
