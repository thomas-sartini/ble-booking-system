package ch.hslu.blebooking.backend.booking;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Availability grid of a resource (docs/api/booking-api.md, US-02.01). Lives in the booking module because
 * it owns the bookings. Requires authentication.
 */
@RestController
class AvailabilityController {

	private final AvailabilityQuery availabilityQuery;

	AvailabilityController(AvailabilityQuery availabilityQuery) {
		this.availabilityQuery = availabilityQuery;
	}

	@GetMapping("/resources/{id}/availability")
	Availability availability(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return availabilityQuery.availability(id, from, to, Long.parseLong(jwt.getSubject()),
				jwt.getClaimAsString("role"));
	}

}
