package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.request.AccessConfirmationRequestDto
import ch.hslu.blebooking.mobile.communication.dto.model.BookingDto
import ch.hslu.blebooking.mobile.communication.dto.request.CheckOutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LogoutRequestDto
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
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 *
 * Executes HTTP requests using transport DTOs.
 */
internal class BackendApiClient(
    private val httpClient: HttpClient
) : ApiClient {


    override suspend fun login(
        request: LoginRequestDto
    ): ApiResult<LoginResponseDto> = executeRequest {
        httpClient.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }


    override suspend fun refresh(
        request: RefreshTokenRequestDto
    ): ApiResult<RefreshTokenResponseDto> = executeRequest {
        httpClient.post("auth/refresh"){
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun logout(request: LogoutRequestDto): ApiResult<Unit> = executeRequest {
        httpClient.post("auth/logout"){
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getActiveBookings(token: String): ApiResult<List<BookingDto>> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAccess(
        token: String,
        request: ResourceAccessRequestDto
    ): ApiResult<ResourceAccessResponseDto> {
        TODO("Not yet implemented")
    }

    override suspend fun confirmAccess(
        token: String,
        request: AccessConfirmationRequestDto
    ): ApiResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun checkOut(
        token: String,
        request: CheckOutRequestDto
    ): ApiResult<Unit> {
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
                is UnresolvedAddressException -> ApiResult.NetworkError(e)

                is ConnectTimeoutException,
                is HttpRequestTimeoutException,
                is SocketTimeoutException -> ApiResult.Timeout(e)

                is ContentConvertException,
                is SerializationException,
                is NoTransformationFoundException -> ApiResult.DecodingError(e)

                else -> ApiResult.UnexpectedError(e)
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
            val errorBody = response.bodyAsText()
            val detail = try {
                val payload = Json.parseToJsonElement(errorBody) as? JsonObject
                (payload?.get("detail") as? JsonPrimitive)
                    ?.contentOrNull?.takeIf { it.isNotBlank() }
            } catch (_: IllegalArgumentException) {
                null
            }
            ApiResult.HttpError(
                status = response.status,
                message = detail ?: errorBody
            )
        }
    }

}
