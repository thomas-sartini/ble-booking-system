package ch.hslu.blebooking.backend.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import ch.hslu.blebooking.backend.booking.BookingDto;
import ch.hslu.blebooking.backend.booking.BookingResource;
import ch.hslu.blebooking.backend.booking.BookingStatus;
import ch.hslu.blebooking.backend.notification.internal.BookingMailComposer.Kind;
import ch.hslu.blebooking.backend.notification.internal.BookingMailComposer.Mail;
import ch.hslu.blebooking.backend.resource.ResourceType;

class BookingMailComposerTest {

	private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

	@Test
	void winterTimesAreShownInZurichTime() {
		// 07:00Z = 08:00 CET (+01:00)
		Mail mail = BookingMailComposer.compose(Kind.CONFIRMED, "Anna",
				booking("2026-11-17T07:00:00Z", "2026-11-17T08:30:00Z", "Building D, Room 2.14", "18.75"), ZURICH);

		assertThat(mail.subject()).isEqualTo("Booking confirmed: Project Room on Tuesday, 17 November 2026");
		assertThat(mail.body())
			.startsWith("Hello Anna,\n\nyour booking is confirmed.\n")
			.contains("Location:   Building D, Room 2.14\n")
			.contains("Date:       Tuesday, 17 November 2026\n")
			.contains("Time:       08:00–09:30 (Europe/Zurich)\n")
			.contains("Price:      CHF 18.75\n")
			.contains("Booking ID: 42\n");
	}

	@Test
	void summerTimesAreShownInZurichTime() {
		// 06:00Z = 08:00 CEST (+02:00)
		Mail mail = BookingMailComposer.compose(Kind.CANCELLED, "Ben",
				booking("2027-07-05T06:00:00Z", "2027-07-05T06:30:00Z", null, "0.00"), ZURICH);

		assertThat(mail.subject()).isEqualTo("Booking cancelled: Project Room on Monday, 5 July 2027");
		assertThat(mail.body())
			.contains("your booking has been cancelled.")
			.contains("Location:   -\n")
			.contains("Time:       08:00–08:30 (Europe/Zurich)\n")
			.contains("Price:      CHF 0.00\n");
	}

	private static BookingDto booking(String start, String end, String position, String price) {
		return new BookingDto(42, new BookingResource(3, "Project Room", ResourceType.FIXED, position),
				OffsetDateTime.parse(start), OffsetDateTime.parse(end), BookingStatus.BOOKED, price, "CHF", true,
				OffsetDateTime.parse("2026-11-16T10:00:00Z"));
	}

}
