package ch.hslu.blebooking.backend.notification.internal;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import ch.hslu.blebooking.backend.booking.BookingCancelled;
import ch.hslu.blebooking.backend.booking.BookingConfirmed;
import ch.hslu.blebooking.backend.booking.BookingService;
import ch.hslu.blebooking.backend.booking.BookingStatus;
import ch.hslu.blebooking.backend.booking.OwnedBooking;
import ch.hslu.blebooking.backend.common.config.BookingProperties;
import ch.hslu.blebooking.backend.notification.internal.BookingMailComposer.Kind;
import ch.hslu.blebooking.backend.notification.internal.BookingMailComposer.Mail;
import ch.hslu.blebooking.backend.user.UserDto;
import ch.hslu.blebooking.backend.user.UserManagementService;

/**
 * Sends the confirmation and cancellation emails (FR-07.01). Runs asynchronously after the booking transaction
 * committed, so a mail failure never undoes a booking or cancellation.
 * <p>
 * A failed delivery is logged with the booking id only and rethrown, so the event publication stays incomplete
 * in {@code event_publication} and is re-submitted on the next restart (FR-07.02, backend/README.md).
 */
@Component
class BookingMailListener {

	private static final Logger log = LoggerFactory.getLogger(BookingMailListener.class);

	private final BookingService bookingService;
	private final UserManagementService userManagementService;
	private final ObjectProvider<JavaMailSender> mailSender;
	private final MailProperties mailProperties;
	private final BookingProperties bookingProperties;
	private final String mailHost;

	BookingMailListener(BookingService bookingService, UserManagementService userManagementService,
			ObjectProvider<JavaMailSender> mailSender, MailProperties mailProperties,
			BookingProperties bookingProperties, @Value("${spring.mail.host:}") String mailHost) {
		this.bookingService = bookingService;
		this.userManagementService = userManagementService;
		this.mailSender = mailSender;
		this.mailProperties = mailProperties;
		this.bookingProperties = bookingProperties;
		this.mailHost = mailHost;
	}

	@ApplicationModuleListener
	void on(BookingConfirmed event) {
		send(Kind.CONFIRMED, event.bookingId());
	}

	@ApplicationModuleListener
	void on(BookingCancelled event) {
		send(Kind.CANCELLED, event.bookingId());
	}

	private void send(Kind kind, long bookingId) {
		if (!mailProperties.enabled()) {
			log.info("mail disabled, skipped booking {}", bookingId);
			return;
		}

		Optional<OwnedBooking> owned = bookingService.findById(bookingId);
		if (owned.isEmpty()) {
			log.warn("Booking {} no longer exists, no {} mail sent", bookingId, kind);
			return;
		}
		// a re-submitted confirmation must not arrive after the booking was cancelled
		if (kind == Kind.CONFIRMED && owned.get().booking().status() != BookingStatus.BOOKED) {
			log.info("Booking {} is no longer BOOKED, no {} mail sent", bookingId, kind);
			return;
		}
		Optional<UserDto> owner = userManagementService.getUserById(owned.get().ownerId());
		if (owner.isEmpty()) {
			log.warn("Owner of booking {} no longer exists, no {} mail sent", bookingId, kind);
			return;
		}

		Mail mail = BookingMailComposer.compose(kind, owner.get().firstName(), owned.get().booking(),
				bookingProperties.zone());
		try {
			JavaMailSender sender = mailSender.getIfAvailable();
			if (sender == null || mailHost.isBlank()) {
				throw new IllegalStateException("no mail server configured (SPRING_MAIL_HOST)");
			}
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(mailProperties.from());
			message.setTo(owner.get().email());
			message.setSubject(mail.subject());
			message.setText(mail.body());
			sender.send(message);
			log.info("Sent {} mail for booking {}", kind, bookingId);
		}
		catch (RuntimeException e) {
			// only the booking id and the error type: SMTP error texts can contain the recipient's address
			log.error("Could not send {} mail for booking {} ({}), will be retried on restart", kind, bookingId,
					e.getClass().getSimpleName());
			throw new MailDeliveryFailedException(kind, bookingId);
		}
	}

	/** Keeps the event publication incomplete. Has no cause, so no address or mail content reaches the logs. */
	static class MailDeliveryFailedException extends RuntimeException {

		MailDeliveryFailedException(Kind kind, long bookingId) {
			super("Could not send " + kind + " mail for booking " + bookingId);
		}

	}

}
