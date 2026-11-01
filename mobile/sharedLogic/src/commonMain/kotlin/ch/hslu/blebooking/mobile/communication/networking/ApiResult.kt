package ch.hslu.blebooking.mobile.communication.networking


import io.ktor.http.HttpStatusCode

sealed interface ApiResult<out T> {

    data class Success<T>(
        val status: HttpStatusCode,
        val data: T
    ) : ApiResult<T>

    data class Error(
        val status: HttpStatusCode? = null,
        val message: String
    ) : ApiResult<Nothing>
}
