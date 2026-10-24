package ch.hslu.blebooking.backend.audit.internal;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps the table audit_log. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "actor_user_id")
	private Long actorUserId;
	@Column(name = "action")
	private String action;
	@Column(name = "target_entity")
	private String targetEntity;
	@Column(name = "\"timestamp\"")
	private Instant timestamp;
	@Column(name = "details")
	@JdbcTypeCode(SqlTypes.JSON)
	private String details;
	@Column(name = "target_id")
	private Long targetId;
	@Column(name = "outcome")
	private String outcome;
	@Column(name = "ip_address")
	private String ipAddress;

	protected AuditLog() {
		// for JPA
	}

	public Long getId() {
		return id;
	}

	public Long getActorUserId() {
		return actorUserId;
	}

	public String getAction() {
		return action;
	}

	public String getTargetEntity() {
		return targetEntity;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public String getDetails() {
		return details;
	}

	public Long getTargetId() {
		return targetId;
	}

	public String getOutcome() {
		return outcome;
	}

	public String getIpAddress() {
		return ipAddress;
	}

}
