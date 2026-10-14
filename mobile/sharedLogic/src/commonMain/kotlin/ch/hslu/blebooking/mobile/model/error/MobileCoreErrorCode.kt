
package ch.hslu.blebooking.mobile.model.error

/**
 * Platform-independent error codes exposed by MobileCoreApi.
 */
enum class MobileCoreErrorCode {

    // Backend errors
    VALIDATION_ERROR,
    INVALID_RECEIPT,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    BOOKING_OVERLAP,
    INVALID_STATUS,
    TOO_MANY_REQUESTS,
    INTERNAL_ERROR,

    // Communication errors
    CONNECTION_ERROR,
    TIMEOUT,

    // Unrecognized errors
    UNKNOWN_ERROR
}


