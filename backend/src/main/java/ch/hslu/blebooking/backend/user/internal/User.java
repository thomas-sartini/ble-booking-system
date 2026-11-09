package ch.hslu.blebooking.backend.user.internal;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ch.hslu.blebooking.backend.user.Role;
import ch.hslu.blebooking.backend.user.UserStatus;

/**
 * Maps the table "user". Other modules refer to it only by its id.
 */
@Entity
@Table(name = "\"user\"")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "email")
	private String email;
	@Column(name = "username")
	private String username;
	@Column(name = "password_hash")
	private String passwordHash;
	@Column(name = "role")
	@Enumerated(EnumType.STRING)
	private Role role;
	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private UserStatus status;
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
	@Column(name = "first_name")
	private String firstName;
	@Column(name = "last_name")
	private String lastName;
	@Column(name = "start_semester")
	private String startSemester;
	@Column(name = "field_of_study")
	private String fieldOfStudy;
	@Column(name = "failed_attempts")
	private Long failedAttempts;
	@Column(name = "locked_until")
	private Instant lockedUntil;

	public User() {
		// for JPA and controlled creation from other modules
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getStartSemester() {
		return startSemester;
	}

	public String getFieldOfStudy() {
		return fieldOfStudy;
	}

	public Long getFailedAttempts() {
		return failedAttempts;
	}

	public void setFailedAttempts(Long failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public void setLockedUntil(Instant lockedUntil) {
		this.lockedUntil = lockedUntil;
	}

}
