package ch.hslu.blebooking.backend.notification.internal;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ch.hslu.blebooking.backend.booking.BookingDto;

/**
 * Builds the plain text of the booking emails (English). Contains only the owner's first name and the booking
 * itself: no links, tokens, passwords or data of other users (D14).
 */
final class BookingMailComposer {

	enum Kind {
		CONFIRMED("Booking confirmed", "your booking is confirmed."),
		CANCELLED("Booking cancelled", "your booking has been cancelled.");

		private final String subject;
		private final String sentence;

		Kind(String subject, String sentence) {
			this.subject = subject;
			this.sentence = sentence;
		}
	}

	record Mail(String subject, String body) {
	}

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private BookingMailComposer() {
	}

	static Mail compose(Kind kind, String firstName, BookingDto booking, ZoneId zone) {
		ZonedDateTime start = booking.start().atZoneSameInstant(zone);
		ZonedDateTime end = booking.end().atZoneSameInstant(zone);
		String date = DATE.format(start);
		String position = booking.resource().position() == null || booking.resource().position().isBlank()
				? "-"
				: booking.resource().position();

		String subject = kind.subject + ": " + booking.resource().name() + " on " + date;
		String body = """
				Hello %s,

				%s

				Resource:   %s
				Location:   %s
				Date:       %s
				Time:       %s–%s (%s)
				Price:      %s %s
				Booking ID: %d

				You can see your bookings in the BLE Booking web app.

				BLE Booking System
				""".formatted(firstName, kind.sentence, booking.resource().name(), position, date,
				TIME.format(start), TIME.format(end), zone.getId(), booking.currency(), booking.price(), booking.id());
		return new Mail(subject, body);
	}

}
