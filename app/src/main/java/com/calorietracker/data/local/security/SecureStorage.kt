package com.calorietracker.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SecretReadResult {
    data class Success(val value: String) : SecretReadResult
    data object Missing : SecretReadResult
    data class Failure(val error: Throwable) : SecretReadResult
}

/**
 * Secure storage backed by the Android Keystore for AES-256-GCM encryption.
 *
 * Encrypted blobs (IV + ciphertext) are persisted as Base64 strings inside a
 * plain SharedPreferences file. The AES key never leaves the hardware-backed
 * Keystore.
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val PREFS_FILE = "calsage_secure_storage"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS_PREFIX = "calsage_secret_"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_IV_LENGTH_BYTES = 12
        const val API_KEY_SECRET = "venice_api_key"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Encrypts [value] with AES-256-GCM using a hardware-backed key and stores
     * the resulting IV+ciphertext as Base64 in SharedPreferences.
     */
    suspend fun saveSecret(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        val secretKey = getOrCreateKey(keyAlias(key))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        // Store IV length (1 byte) + IV + ciphertext as a single Base64 blob
        val blob = ByteArray(1 + iv.size + ciphertext.size)
        blob[0] = iv.size.toByte()
        System.arraycopy(iv, 0, blob, 1, iv.size)
        System.arraycopy(ciphertext, 0, blob, 1 + iv.size, ciphertext.size)

        val encoded = Base64.encodeToString(blob, Base64.NO_WRAP)
        sharedPreferences.edit().putString(key, encoded).apply()
    }

    suspend fun readSecret(key: String): SecretReadResult = withContext(Dispatchers.IO) {
        val encoded = sharedPreferences.getString(key, null)
            ?: return@withContext SecretReadResult.Missing
        try {
            val blob = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = blob[0].toInt() and 0xFF
            val iv = blob.copyOfRange(1, 1 + ivLength)
            val ciphertext = blob.copyOfRange(1 + ivLength, blob.size)

            val secretKey = getOrCreateKey(keyAlias(key))
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            SecretReadResult.Success(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        } catch (e: Exception) {
            SecretReadResult.Failure(e)
        }
    }

    suspend fun getSecret(key: String): String? {
        return when (val result = readSecret(key)) {
            is SecretReadResult.Success -> result.value
            SecretReadResult.Missing,
            is SecretReadResult.Failure -> null
        }
    }

    /**
     * Removes a stored secret and its corresponding Keystore entry.
     */
    suspend fun deleteSecret(key: String): Unit = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(key).apply()
        val alias = keyAlias(key)
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    /**
     * Returns the existing AES-256 key for [alias], or generates a new one
     * inside the Android Keystore if it does not yet exist.
     */
    private fun getOrCreateKey(alias: String): SecretKey {
        val existingEntry = keyStore.getEntry(alias, null)
        if (existingEntry is KeyStore.SecretKeyEntry) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun keyAlias(key: String): String = "$KEY_ALIAS_PREFIX$key"

    // --- API Key convenience methods ---

    suspend fun getApiKey(): String? = getSecret(API_KEY_SECRET)

    suspend fun readApiKey(): SecretReadResult = readSecret(API_KEY_SECRET)

    suspend fun saveApiKey(key: String) = saveSecret(API_KEY_SECRET, key)

    suspend fun clearApiKey() = deleteSecret(API_KEY_SECRET)

    suspend fun clearAll() {
        deleteSecret(API_KEY_SECRET)
    }
}
