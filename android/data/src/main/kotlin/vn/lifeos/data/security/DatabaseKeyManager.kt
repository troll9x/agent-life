package vn.lifeos.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Owns a random SQLCipher secret, wrapped by a non-exportable Android Keystore AES key. */
internal class DatabaseKeyManager(
    context: Context,
    private val alias: String,
    wrappedKeyFileName: String,
) {
    private val applicationContext = context.applicationContext
    private val wrappedFile = File(applicationContext.noBackupFilesDir, wrappedKeyFileName)
    private val atomicFile = AtomicFile(wrappedFile)
    private val random = SecureRandom()

    /** Returns a fresh byte array. The caller must clear it after passing it to SQLCipher. */
    fun loadOrCreate(databaseFile: File): ByteArray {
        val persisted = wrappedFile.exists() || File(wrappedFile.path + ".bak").exists() || File(wrappedFile.path + ".new").exists()
        if (persisted) {
            if (!databaseFile.exists()) throw DatabaseKeyUnavailable()
            return unwrap(atomicFile.readFully())
        }
        if (databaseFile.exists() || File(databaseFile.path + "-wal").exists() || File(databaseFile.path + "-shm").exists()) {
            throw DatabaseKeyUnavailable()
        }
        val wrappingKey = getOrGenerateWrappingKey()
        val secret = ByteArray(SECRET_BYTES).also(random::nextBytes)
        try {
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
            cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
            if (cipher.iv.size != IV_BYTES) throw DatabaseKeyUnavailable()
            val wrapped = MAGIC + cipher.iv + cipher.doFinal(secret)
            val stream = atomicFile.startWrite()
            try {
                stream.write(wrapped)
                atomicFile.finishWrite(stream)
            } catch (error: Exception) {
                atomicFile.failWrite(stream)
                throw error
            }
            return secret
        } catch (error: Exception) {
            secret.fill(0)
            throw error
        }
    }

    private fun unwrap(wrapped: ByteArray): ByteArray {
        if (wrapped.size != MAGIC.size + IV_BYTES + SECRET_BYTES + TAG_BYTES || !wrapped.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw DatabaseKeyUnavailable()
        }
        val key = loadWrappingKey() ?: throw DatabaseKeyUnavailable()
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, wrapped.copyOfRange(MAGIC.size, MAGIC.size + IV_BYTES)))
        cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(wrapped, MAGIC.size + IV_BYTES, wrapped.size - MAGIC.size - IV_BYTES)
            .also { if (it.size != SECRET_BYTES) throw DatabaseKeyUnavailable() }
    }

    private fun loadWrappingKey(): SecretKey? {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return store.getKey(alias, null) as? SecretKey
    }

    private fun getOrGenerateWrappingKey(): SecretKey {
        loadWrappingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setUnlockedDeviceRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        val MAGIC = "ALDBKEY1".toByteArray(Charsets.US_ASCII)
        const val CIPHER = "AES/GCM/NoPadding"
        const val SECRET_BYTES = 32
        const val IV_BYTES = 12
        const val TAG_BYTES = 16
    }
}

internal class DatabaseKeyUnavailable : Exception()
