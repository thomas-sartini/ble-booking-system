package ch.hslu.blebooking.backend.resource.internal;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ch.hslu.blebooking.backend.resource.ResourceStatus;
import ch.hslu.blebooking.backend.resource.ResourceType;

/**
 * Maps the table resource. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "resource")
public class Resource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "name")
	private String name;
	@Column(name = "description")
	private String description;
	@Column(name = "price_per_hour", precision = 10, scale = 2)
	private BigDecimal pricePerHour;
	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private ResourceStatus status;
	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	private ResourceType type;
	@Column(name = "position")
	private String position;

	protected Resource() {
		// for JPA
	}

	Resource(String name, String description, BigDecimal pricePerHour, ResourceStatus status, ResourceType type,
			String position) {
		this.name = name;
		this.description = description;
		this.pricePerHour = pricePerHour;
		this.status = status;
		this.type = type;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPricePerHour() {
		return pricePerHour;
	}

	public ResourceStatus getStatus() {
		return status;
	}

	public ResourceType getType() {
		return type;
	}

	public String getPosition() {
		return position;
	}

}
