package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.dto.response.UserProfileResponseDto
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.model.Role
import ch.hslu.blebooking.mobile.model.UserProfile
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {

    @Test
    fun getUserProfileMapsDtoSuccessfully() = runTest {
        val apiClient = mock<ApiClient>()
        val dto = UserProfileResponseDto(
            id = 1L,
            email = "user@test.ch",
            username = "testuser",
            firstName = "John",
            lastName = "Doe",
            role = "STUDENT"
        )
        everySuspend { apiClient.getUserProfile() } returns ApiResult.Success(HttpStatusCode.OK, dto)

        val service = UserService(apiClient)
        val profile = service.getUserProfile()

        val expected = UserProfile(
            firstName = "John",
            lastName = "Doe",
            username = "testuser",
            email = "user@test.ch",
            role = Role.STUDENT
        )
        assertEquals(expected, profile)
        verifySuspend { apiClient.getUserProfile() }
    }

    @Test
    fun getUserProfileWithCaseInsensitiveRoleMapsSuccessfully() = runTest {
        val apiClient = mock<ApiClient>()
        val dto = UserProfileResponseDto(
            id = 2L,
            email = "staff@test.ch",
            username = "staffuser",
            firstName = "Jane",
            lastName = "Staff",
            role = "staff"
        )
        everySuspend { apiClient.getUserProfile() } returns ApiResult.Success(HttpStatusCode.OK, dto)

        val service = UserService(apiClient)
        val profile = service.getUserProfile()

        assertEquals(Role.STAFF, profile.role)
    }

    @Test
    fun getUserProfileWithUnknownRoleThrowsMobileCoreException() = runTest {
        val apiClient = mock<ApiClient>()
        val dto = UserProfileResponseDto(
            id = 3L,
            email = "admin@test.ch",
            username = "adminuser",
            firstName = "Root",
            lastName = "Admin",
            role = "UNKNOWN_ROLE"
        )
        everySuspend { apiClient.getUserProfile() } returns ApiResult.Success(HttpStatusCode.OK, dto)

        val service = UserService(apiClient)
        val exception = assertFailsWith<MobileCoreException> {
            service.getUserProfile()
        }
        assertEquals(MobileCoreErrorCode.UNKNOWN_ERROR, exception.code)
    }

    @Test
    fun getUserProfileHttpErrorThrowsMobileCoreException() = runTest {
        val apiClient = mock<ApiClient>()
        everySuspend { apiClient.getUserProfile() } returns ApiResult.HttpError(
            status = HttpStatusCode.Unauthorized,
            message = "Invalid session"
        )

        val service = UserService(apiClient)
        val exception = assertFailsWith<MobileCoreException> {
            service.getUserProfile()
        }
        assertEquals(MobileCoreErrorCode.UNAUTHORIZED, exception.code)
        assertEquals("Invalid session", exception.message)
    }

    @Test
    fun getUserProfileNetworkErrorThrowsMobileCoreException() = runTest {
        val apiClient = mock<ApiClient>()
        everySuspend { apiClient.getUserProfile() } returns ApiResult.NetworkError(
            cause = Exception("Network down")
        )

        val service = UserService(apiClient)
        val exception = assertFailsWith<MobileCoreException> {
            service.getUserProfile()
        }
        assertEquals(MobileCoreErrorCode.NETWORK_ERROR, exception.code)
    }
}
