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
	private String status;
	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	private ResourceType type;
	@Column(name = "position")
	private String position;

	protected Resource() {
		// for JPA
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

	public String getStatus() {
		return status;
	}

	public ResourceType getType() {
		return type;
	}

	public String getPosition() {
		return position;
	}

}
