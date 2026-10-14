
package ch.hslu.blebooking.mobile.model.error

/**
 * Represents an application or communication failure
 * occurring during a Mobile Core operation.
 *
 * @property code Platform-independent error code.
 */
class MobileCoreException(
    val code: MobileCoreErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
