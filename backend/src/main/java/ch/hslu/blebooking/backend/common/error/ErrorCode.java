package ch.hslu.blebooking.backend.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Error codes returned in the "code" field of every error response.
 */
public enum ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
	INVALID_RECEIPT(HttpStatus.BAD_REQUEST),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
	FORBIDDEN(HttpStatus.FORBIDDEN),
	NOT_FOUND(HttpStatus.NOT_FOUND),
	BOOKING_OVERLAP(HttpStatus.CONFLICT),
	INVALID_STATUS(HttpStatus.CONFLICT),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}

	/** Code for errors that Spring creates itself, e.g. an unknown path (404). */
	static String forStatus(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> VALIDATION_ERROR.name();
			case 401 -> UNAUTHORIZED.name();
			case 403 -> FORBIDDEN.name();
			case 404 -> NOT_FOUND.name();
			case 500 -> INTERNAL_ERROR.name();
			default -> {
				HttpStatus known = HttpStatus.resolve(status.value());
				yield known != null ? known.name() : INTERNAL_ERROR.name();
			}
		};
	}

}
