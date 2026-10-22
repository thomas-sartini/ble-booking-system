package ch.hslu.blebooking.backend.common.error;

/**
 * Thrown by services for expected errors. Turned into a Problem Details response.
 */
public class ApiException extends RuntimeException {

	private final ErrorCode code;

	public ApiException(ErrorCode code, String detail) {
		super(detail);
		this.code = code;
	}

	public ErrorCode code() {
		return code;
	}

}
