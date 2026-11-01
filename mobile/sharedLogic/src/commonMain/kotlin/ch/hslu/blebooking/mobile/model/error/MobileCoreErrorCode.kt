
package ch.hslu.blebooking.mobile.model.error

/**
 * Platform-independent error codes exposed by MobileCoreApi.
 */

enum class MobileCoreErrorCode(
    val httpStatus: Int?,
    val message: String
) {

    //Communication errors
    BAD_REQUEST(400, "Invalid request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Access forbidden"),
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "Resource conflict"),
    TOO_MANY_REQUESTS(429, "Too many requests"),
    SERVER_ERROR(500, "Internal server error"),

    NETWORK_ERROR(503, "Network error"),
    UNKNOWN_ERROR(null, "Unknown error"),


    // logic errors
    VALIDATION_ERROR(null, "Validation error");

    companion object {
        fun fromHttpStatus(status: Int): MobileCoreErrorCode {
            return entries.firstOrNull {
                it.httpStatus == status
            } ?: when (status) {
                in 500..599 -> SERVER_ERROR
                else -> UNKNOWN_ERROR
            }
        }
    }

}

