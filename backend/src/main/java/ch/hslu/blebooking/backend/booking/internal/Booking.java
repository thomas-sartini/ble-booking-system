package ch.hslu.blebooking.backend.booking.internal;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ch.hslu.blebooking.backend.booking.BookingStatus;

/**
 * Maps the table booking. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "booking")
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id")
	private Long userId;
	@Column(name = "resource_id")
	private Long resourceId;
	@Column(name = "start_time")
	private Instant startTime;
	@Column(name = "end_time")
	private Instant endTime;
	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private BookingStatus status;
	@Column(name = "locked_price", precision = 10, scale = 2)
	private BigDecimal lockedPrice;
	/** Set by the application at insert time (contract §4). */
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	protected Booking() {
		// for JPA
	}

	/** A new booking in status BOOKED. */
	Booking(Long userId, Long resourceId, Instant startTime, Instant endTime, BigDecimal lockedPrice) {
		this.userId = userId;
		this.resourceId = resourceId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.status = BookingStatus.BOOKED;
		this.lockedPrice = lockedPrice;
		this.createdAt = Instant.now();
	}

	/** D12: the caller checks that cancelling is allowed. */
	void cancel() {
		this.status = BookingStatus.CANCELLED;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getResourceId() {
		return resourceId;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public BigDecimal getLockedPrice() {
		return lockedPrice;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
