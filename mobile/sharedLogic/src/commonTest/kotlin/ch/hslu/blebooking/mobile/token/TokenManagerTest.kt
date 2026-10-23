
package ch.hslu.blebooking.mobile.token

import ch.hslu.blebooking.mobile.api.SecureStorage
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenManagerTest {

    @Test
    @Ignore // TokenManager not implemented yet
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
    @Ignore
    fun testGetRefreshTokenReturnsStoredToken() {
        // Arrange
        val storage = mock<SecureStorage>()
        val expectedToken = "refresh-token"
        every {
            storage.get(any())
        } returns expectedToken

        val manager = TokenManager(storage)

        // Act
        val result = manager.getRefreshToken()

        // Assert
        assertEquals(expectedToken, result)

        verify(VerifyMode.exactly(1)) {
            storage.get(any())
        }
    }

    @Test
    @Ignore
    fun testGetRefreshTokenReturnsNullWhenMissing() {
        // Arrange
        val storage = mock<SecureStorage>()

        every {
            storage.get(any())
        } returns null

        val manager = TokenManager(storage)

        // Act
        val result = manager.getRefreshToken()

        // Assert
        assertNull(result)

        verify(VerifyMode.exactly(1)) {
            storage.get(any())
        }
    }

    @Test
    @Ignore
    fun testStoreTokensStoresCorrectly() {
        // Arrange
        val storage = mock<SecureStorage>()

        every {
            storage.save(any(), any())
        } returns Unit

        val manager = TokenManager(storage)

        // Act
        manager.storeTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        // Assert
        assertEquals(
            "access-token",
            manager.getAccessToken()
        )
        assertEquals(
            "refresh-token",
            manager.getRefreshToken()
        )
    }

    @Test
    @Ignore
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

    }

    @Test
    @Ignore
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

    }
}
