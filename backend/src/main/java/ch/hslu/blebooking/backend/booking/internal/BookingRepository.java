package ch.hslu.blebooking.backend.booking.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ch.hslu.blebooking.backend.booking.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/** Bookings of a resource in the given statuses that overlap [rangeStart, rangeEnd). */
	List<Booking> findByResourceIdAndStatusInAndStartTimeBeforeAndEndTimeAfter(Long resourceId,
			Collection<BookingStatus> statuses, Instant rangeEnd, Instant rangeStart);

}
