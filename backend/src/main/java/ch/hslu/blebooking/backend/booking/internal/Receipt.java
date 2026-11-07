package ch.hslu.blebooking.backend.booking.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the table receipt. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "receipt")
public class Receipt {

	@Id
	@GeneratedValue
	private UUID id;
	@Column(name = "booking_id")
	private Long bookingId;
	@Column(name = "terminal_id")
	private Long terminalId;
	@Column(name = "ticket_id")
	private Long ticketId;
	@Column(name = "type")
	private String type;
	@Column(name = "receipt_time")
	private Instant receiptTime;
	@Column(name = "signature")
	private String signature;
	@Column(name = "received_at", insertable = false, updatable = false)
	private Instant receivedAt;

	protected Receipt() {
		// for JPA
	}

	public UUID getId() {
		return id;
	}

	public Long getBookingId() {
		return bookingId;
	}

	public Long getTerminalId() {
		return terminalId;
	}

	public Long getTicketId() {
		return ticketId;
	}

	public String getType() {
		return type;
	}

	public Instant getReceiptTime() {
		return receiptTime;
	}

	public String getSignature() {
		return signature;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}

}
