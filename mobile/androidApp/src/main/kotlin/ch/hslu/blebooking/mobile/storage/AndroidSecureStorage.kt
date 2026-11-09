
package ch.hslu.blebooking.mobile.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ch.hslu.blebooking.mobile.api.SecureStorage
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecureStorage(
    context: Context
) : SecureStorage {

    private val storageDir = context.noBackupFilesDir

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(
            KEY_ALIAS,
            null
        ) as? KeyStore.SecretKeyEntry

        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            ALGORITHM,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    private fun getEncryptCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }
    }

    private fun getDecryptCipherForIv(
        iv: ByteArray
    ): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                getKey(),
                GCMParameterSpec(GCM_TAG_LENGTH, iv)
            )
        }
    }

    private fun getFile(key: String): File {
        val hash = MessageDigest
            .getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }

        return File(storageDir, "secure_$hash.dat")
    }

    private fun saveEncryptedData(
        key: String,
        iv: ByteArray,
        encryptedBytes: ByteArray
    ) {
        DataOutputStream(getFile(key).outputStream()).use { output ->
            output.writeInt(iv.size)
            output.write(iv)

            output.writeInt(encryptedBytes.size)
            output.write(encryptedBytes)
        }
    }

    private fun getEncryptedData(
        key: String
    ): Pair<ByteArray, ByteArray>? {
        val file = getFile(key)

        return try {
            DataInputStream(file.inputStream()).use { input ->
                val ivSize = input.readInt()
                require(ivSize == IV_SIZE) {
                    "Invalid IV size"
                }

                val iv = ByteArray(ivSize)
                input.readFully(iv)

                val encryptedSize = input.readInt()
                require(encryptedSize in 16..MAX_ENCRYPTED_SIZE) {
                    "Invalid encrypted data size"
                }

                val encryptedBytes = ByteArray(encryptedSize)
                input.readFully(encryptedBytes)

                Pair(iv, encryptedBytes)
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }

    override fun save(key: String, value: String) {
        synchronized(storageLock) {
            val cipher = getEncryptCipher()

            val encryptedBytes = cipher.doFinal(
                value.toByteArray(Charsets.UTF_8)
            )

            saveEncryptedData(
                key = key,
                iv = cipher.iv,
                encryptedBytes = encryptedBytes
            )
        }
    }

    override fun get(key: String): String? {
        synchronized(storageLock) {
            val (iv, encryptedBytes) =
                getEncryptedData(key) ?: return null

            val cipher = getDecryptCipherForIv(iv)

            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return String(decryptedBytes, Charsets.UTF_8)
        }
    }

    override fun remove(key: String) {
        synchronized(storageLock) {
            val file = getFile(key)

            if (file.exists() && !file.delete()) {
                throw IllegalStateException("Cannot delete secure storage file")
            }
        }
    }

    private companion object {
        private val storageLock = Any()

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "secure_storage_aes_key"

        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE

        private const val TRANSFORMATION =
            "$ALGORITHM/$BLOCK_MODE/$PADDING"

        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
        private const val MAX_ENCRYPTED_SIZE = 1_048_576
    }
}
