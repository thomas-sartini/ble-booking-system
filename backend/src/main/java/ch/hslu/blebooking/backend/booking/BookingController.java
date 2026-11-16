package ch.hslu.blebooking.backend.booking;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking endpoints (docs/api/booking-api.md, US-02.02 and US-02.03). Requires authentication.
 * Users only see and cancel their own bookings (D13).
 */
@RestController
@RequestMapping("/bookings")
class BookingController {

	private final BookingService bookingService;

	BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	ResponseEntity<BookingDto> create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateBookingRequest request) {
		BookingDto booking = bookingService.create(request, userId(jwt), jwt.getClaimAsString("role"));
		return ResponseEntity.created(URI.create("/bookings/" + booking.id())).body(booking);
	}

	@GetMapping
	List<BookingDto> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "all") String scope) {
		return bookingService.list(userId(jwt), scope);
	}

	@GetMapping("/{id}")
	BookingDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
		return bookingService.get(id, userId(jwt));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
		bookingService.cancel(id, userId(jwt));
		return ResponseEntity.noContent().build();
	}

	private static long userId(Jwt jwt) {
		return Long.parseLong(jwt.getSubject());
	}

}
