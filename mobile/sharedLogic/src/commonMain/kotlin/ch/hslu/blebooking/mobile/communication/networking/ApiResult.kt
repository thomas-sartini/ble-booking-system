package ch.hslu.blebooking.mobile.communication.networking

import io.ktor.http.HttpStatusCode

internal sealed interface ApiResult<out T> {
    data class Success<T>(
        val status: HttpStatusCode,
        val data: T
    ) : ApiResult<T>

    sealed interface Error : ApiResult<Nothing> {
        val message: String
        val cause: Throwable?
    }

    /** An actual HTTP error response from the backend. */
    data class HttpError(
        val status: HttpStatusCode,
        override val message: String
    ) : Error {
        override val cause: Throwable? = null
    }

    data class NetworkError(
        override val cause: Throwable,
        override val message: String = "Unable to connect to the server"
    ) : Error

    data class Timeout(
        override val cause: Throwable,
        override val message: String = "Connection to the server timed out"
    ) : Error

    data class DecodingError(
        override val cause: Throwable,
        override val message: String = "Failed to process the server response"
    ) : Error

    data class UnexpectedError(
        override val cause: Throwable,
        override val message: String = cause.message ?: "An unexpected error occurred"
    ) : Error
}
