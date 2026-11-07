package ch.hslu.blebooking.backend.invoice.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the table invoice. Other modules refer to it only by its id.
 */
@Entity
@Table(name = "invoice")
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "invoice_number")
	private String invoiceNumber;
	@Column(name = "booking_id")
	private Long bookingId;
	@Column(name = "user_id")
	private Long userId;
	@Column(name = "final_price", precision = 10, scale = 2)
	private BigDecimal finalPrice;
	@Column(name = "pdf_file_path")
	private String pdfFilePath;
	@Column(name = "status")
	private String status;
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
	@Column(name = "type")
	private String type;
	@Column(name = "qr_reference")
	private String qrReference;
	@Column(name = "due_date")
	private LocalDate dueDate;
	@Column(name = "paid_at")
	private Instant paidAt;
	@Column(name = "currency")
	private String currency;

	protected Invoice() {
		// for JPA
	}

	public Long getId() {
		return id;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public Long getBookingId() {
		return bookingId;
	}

	public Long getUserId() {
		return userId;
	}

	public BigDecimal getFinalPrice() {
		return finalPrice;
	}

	public String getPdfFilePath() {
		return pdfFilePath;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getType() {
		return type;
	}

	public String getQrReference() {
		return qrReference;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

	public String getCurrency() {
		return currency;
	}

}
