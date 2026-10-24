package ch.hslu.blebooking.backend.resource.internal;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the table availability. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "availability")
public class Availability {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "resource_id")
	private Long resourceId;
	@Column(name = "day_of_week")
	private Short dayOfWeek;
	@Column(name = "start_time")
	private LocalTime startTime;
	@Column(name = "end_time")
	private LocalTime endTime;
	@Column(name = "permitted_role")
	private String permittedRole;

	protected Availability() {
		// for JPA
	}

	public Long getId() {
		return id;
	}

	public Long getResourceId() {
		return resourceId;
	}

	public Short getDayOfWeek() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public String getPermittedRole() {
		return permittedRole;
	}

}
