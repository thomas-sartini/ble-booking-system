package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.request.AccessConfirmationRequestDto
import ch.hslu.blebooking.mobile.communication.dto.model.BookingDto
import ch.hslu.blebooking.mobile.communication.dto.request.CheckOutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.LoginResponseDto
import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.RefreshTokenResponseDto
import ch.hslu.blebooking.mobile.communication.dto.request.ResourceAccessRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.ResourceAccessResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Low-level REST client used by [ch.hslu.blebooking.mobile.communication.BackendCommunication].
 *
 * Executes HTTP requests using transport DTOs.
 */
internal class BackendApiClient(
    private val httpClient: HttpClient
) : ApiClient {


    override suspend fun login(
        request: LoginRequestDto
    ): ApiResult<LoginResponseDto> = executeRequest {
        httpClient.post("login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }


    override suspend fun refresh(request: RefreshTokenRequestDto): RefreshTokenResponseDto {
        TODO("Not yet implemented")
    }

    override suspend fun logout(refreshToken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getActiveBookings(token: String): List<BookingDto> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAccess(
        token: String,
        request: ResourceAccessRequestDto
    ): ResourceAccessResponseDto {
        TODO("Not yet implemented")
    }

    override suspend fun confirmAccess(
        token: String,
        request: AccessConfirmationRequestDto
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun checkOut(token: String, request: CheckOutRequestDto) {
        TODO("Not yet implemented")
    }





    private suspend inline fun <reified T> executeRequest(
        crossinline request: suspend () -> HttpResponse
    ): ApiResult<T> {
        return try {
            parseResponse(request())

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            when (e) {
                is UnresolvedAddressException -> ApiResult.Error(
                    status = HttpStatusCode.ServiceUnavailable,
                    message = "Unable to connect to the server"
                )

                is ConnectTimeoutException,
                is HttpRequestTimeoutException,
                is SocketTimeoutException -> ApiResult.Error(
                    status = HttpStatusCode.GatewayTimeout,
                    message = "Connection to the server timed out"
                )

                is ContentConvertException,
                is SerializationException,
                is NoTransformationFoundException -> ApiResult.Error(
                    status = HttpStatusCode.BadGateway,
                    message = "Failed to process the server response"
                )

                else -> ApiResult.Error(
                    status = HttpStatusCode.InternalServerError,
                    message = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }



    private suspend inline fun <reified T> parseResponse(
        response: HttpResponse
    ): ApiResult<T> {
        return if (response.status.value in 200..299) {
            ApiResult.Success(
                status = response.status,
                data = response.body<T>()
            )
        } else {
            ApiResult.Error(
                status = response.status,
                message = response.bodyAsText()
            )
        }
    }

}
