package ch.hslu.blebooking.backend.booking.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import ch.hslu.blebooking.backend.booking.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/** Bookings of a resource in the given statuses that overlap [rangeStart, rangeEnd). */
	List<Booking> findByResourceIdAndStatusInAndStartTimeBeforeAndEndTimeAfter(Long resourceId,
			Collection<BookingStatus> statuses, Instant rangeEnd, Instant rangeStart);

	List<Booking> findByUserId(Long userId);

	Optional<Booking> findByIdAndUserId(Long id, Long userId);

	/** Same as {@link #findByIdAndUserId}, but locks the row until the transaction ends. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Booking b where b.id = :id and b.userId = :userId")
	Optional<Booking> findForUpdate(Long id, Long userId);

}
