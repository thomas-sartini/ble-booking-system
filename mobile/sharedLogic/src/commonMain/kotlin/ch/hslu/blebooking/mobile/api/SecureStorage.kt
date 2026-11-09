package ch.hslu.blebooking.mobile.api

/**
 * Platform-independent abstraction for secure, persistent storage.
 *
 * Implemented natively by each platform (e.g. EncryptedSharedPreferences /
 * Keystore on Android, Keychain Services on iOS).
 *
 * Used primarily to persist sensitive, long-lived credentials (such as refresh tokens).
 * Short-lived access tokens or transient session data must be managed in memory
 * and should not be stored here.
 *
 * Implementations should throw a runtime exception if an unrecoverable hardware or
 * OS security service failure occurs during read/write operations.
 */
interface SecureStorage {

    /**
     * Securely persists a key-value pair.
     * Overwrites any existing value associated with the specified [key].
     *
     * @param key Unique identifier for the stored entry.
     * @param value The sensitive string data to persist securely.
     */
    fun save(key: String, value: String)

    /**
     * Retrieves the securely stored value associated with the specified [key].
     *
     * @param key Unique identifier for the stored entry.
     * @return The stored value, or null if no value exists for the key.
     */
    fun get(key: String): String?

    /**
     * Removes the entry associated with the specified [key] from secure storage.
     * Does nothing if the key does not exist.
     *
     * @param key Unique identifier for the entry to delete.
     */
    fun remove(key: String)
}
