package ch.hslu.blebooking.backend.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;

/**
 * Endpoints that only exist in tests, because the backend has no real endpoints yet.
 * Only loaded by tests that import this class.
 */
@TestComponent
@RestController
@RequestMapping("/test")
public class TestEndpoints {

	public record Input(@NotBlank String name) {
	}

	@GetMapping("/authenticated")
	public String authenticated() {
		return "ok";
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public String admin() {
		return "ok";
	}

	@GetMapping("/api-error")
	public void apiError() {
		throw new ApiException(ErrorCode.BOOKING_OVERLAP, "The resource is already booked at this time.");
	}

	@GetMapping("/unexpected")
	public void unexpected() {
		throw new IllegalStateException("internal detail that must not leak");
	}

	@PostMapping("/validation")
	public String validation(@Valid @RequestBody Input input) {
		return input.name();
	}

}
