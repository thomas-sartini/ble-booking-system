package ch.hslu.blebooking.mobile.api

class NativeStorageResult(
    val value: String?,
    val errorMessage: String?
)

interface NativeStorageDelegate {
    fun save(key: String, value: String): NativeStorageResult
    fun get(key: String): NativeStorageResult
    fun remove(key: String): NativeStorageResult
}

class NativeSecureStorageAdapter(
    private val delegate: NativeStorageDelegate
) : SecureStorage {

    override fun save(key: String, value: String) {
        checkResult(delegate.save(key, value))
    }

    override fun get(key: String): String? {
        return checkResult(delegate.get(key))
    }

    override fun remove(key: String) {
        checkResult(delegate.remove(key))
    }

    private fun checkResult(result: NativeStorageResult): String? {
        result.errorMessage?.let { message ->
            throw IllegalStateException(message)
        }
        return result.value
    }
}