package ch.hslu.blebooking.mobile.token

import ch.hslu.blebooking.mobile.api.SecureStorage
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TokenManagerTest {

    @Test
    fun testGetAccessTokenReturnsNullInitially() {
        // Arrange
        val storage = mock<SecureStorage>()
        val manager = TokenManager(storage)

        // Act
        val result = manager.getAccessToken()

        // Assert
        assertNull(result)
    }

    @Test
    fun testGetRefreshTokenReturnsStoredToken() {
        // Arrange
        val storage = mock<SecureStorage>()
        val expectedToken = "refresh-token"
        every {
            storage.get("refreshToken")
        } returns expectedToken

        val manager = TokenManager(storage)

        // Act
        val result = manager.getRefreshToken()

        // Assert
        assertEquals(expectedToken, result)
        verify(VerifyMode.exactly(1)) {
            storage.get("refreshToken")
        }
    }

    @Test
    fun testGetRefreshTokenReturnsNullWhenMissing() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.get("refreshToken")
        } returns null

        val manager = TokenManager(storage)

        // Act
        val result = manager.getRefreshToken()

        // Assert
        assertNull(result)
        verify(VerifyMode.exactly(1)) {
            storage.get("refreshToken")
        }
    }

    @Test
    fun testGetRefreshTokenThrowsMobileCoreExceptionOnStorageFailure() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.get("refreshToken")
        } throws RuntimeException("Hardware keystore error")

        val manager = TokenManager(storage)

        // Act & Assert
        val exception = assertFailsWith<MobileCoreException> {
            manager.getRefreshToken()
        }
        assertEquals(MobileCoreErrorCode.STORAGE_ERROR, exception.code)
    }

    @Test
    fun testStoreTokensStoresCorrectly() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.save(any(), any())
        } returns Unit
        every {
            storage.get("refreshToken")
        } returns "refresh-token"

        val manager = TokenManager(storage)

        // Act
        manager.storeTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        // Assert
        assertEquals("access-token", manager.getAccessToken())
        assertEquals("refresh-token", manager.getRefreshToken())

        verify(VerifyMode.exactly(1)) {
            storage.save("refreshToken", "refresh-token")
        }
        verify(VerifyMode.exactly(0)) {
            storage.save("accessToken", any())
        }
    }

    @Test
    fun testStoreTokensReplacesExistingTokens() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.save(any(), any())
        } returns Unit

        val manager = TokenManager(storage)

        // Act
        manager.storeTokens("access-1", "refresh-1")
        manager.storeTokens("access-2", "refresh-2")

        // Assert
        assertEquals("access-2", manager.getAccessToken())
        verify(VerifyMode.exactly(1)) {
            storage.save("refreshToken", "refresh-2")
        }
    }

    @Test
    fun testStoreTokensThrowsMobileCoreExceptionOnStorageFailure() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.save(any(), any())
        } throws RuntimeException("Disk or keystore write error")

        val manager = TokenManager(storage)

        // Act & Assert
        val exception = assertFailsWith<MobileCoreException> {
            manager.storeTokens("access", "refresh")
        }
        assertEquals(MobileCoreErrorCode.STORAGE_ERROR, exception.code)
    }

    @Test
    fun testClearTokensRemovesTokens() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.save(any(), any())
        } returns Unit
        every {
            storage.remove(any())
        } returns Unit

        val manager = TokenManager(storage)
        manager.storeTokens("access-token", "refresh-token")

        // Act
        manager.clearTokens()

        // Assert
        assertNull(manager.getAccessToken())
        verify(VerifyMode.exactly(1)) {
            storage.remove("refreshToken")
        }
    }

    @Test
    fun testClearTokensThrowsMobileCoreExceptionOnStorageFailure() {
        // Arrange
        val storage = mock<SecureStorage>()
        every {
            storage.remove(any())
        } throws RuntimeException("Delete error")

        val manager = TokenManager(storage)

        // Act & Assert
        val exception = assertFailsWith<MobileCoreException> {
            manager.clearTokens()
        }
        assertEquals(MobileCoreErrorCode.STORAGE_ERROR, exception.code)
    }
}
