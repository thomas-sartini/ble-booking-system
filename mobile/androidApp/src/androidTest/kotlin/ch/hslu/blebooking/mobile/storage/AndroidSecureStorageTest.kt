package ch.hslu.blebooking.mobile.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidSecureStorageTest {
    // Keep test files separate from the application's stored credentials.
    private val context =InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = AndroidSecureStorage(context)
    private val prefix = "storage-test-${UUID.randomUUID()}"
    private val keys = mutableListOf<String>()

    private fun key(name: String): String = "$prefix-$name".also { keys.add(it) }

    @After
    fun cleanUp() {
        keys.forEach(storage::remove)
    }

    @Test
    fun savesAndReadsUnicodeValue() {
        val key = key("unicode")
        storage.save(key, "token-is-🔑")
        assertEquals("token-is-🔑", storage.get(key))
    }

    @Test
    fun overwritesExistingValue() {
        val key = key("overwrite")
        storage.save(key, "old-token")
        storage.save(key, "new-token")
        assertEquals("new-token", storage.get(key))
    }

    @Test
    fun removesValueAndAllowsRepeatedRemoval() {
        val key = key("remove")
        storage.save(key, "token")
        storage.remove(key)
        storage.remove(key)
        assertNull(storage.get(key))
    }

    @Test
    fun returnsNullForMissingKey() {
        assertNull(storage.get(key("missing")))
    }

    @Test
    fun readsValueFromAnotherInstance() {
        val key = key("persistent")
        storage.save(key, "token")
        assertEquals("token", AndroidSecureStorage(context).get(key))
    }


}
