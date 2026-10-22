package ch.hslu.blebooking.backend.resource.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the table terminal. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "terminal")
public class Terminal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "public_key")
	private String publicKey;
	@Column(name = "status")
	private String status;
	@Column(name = "resource_id")
	private Long resourceId;

	protected Terminal() {
		// for JPA
	}

	public Long getId() {
		return id;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getStatus() {
		return status;
	}

	public Long getResourceId() {
		return resourceId;
	}

}
