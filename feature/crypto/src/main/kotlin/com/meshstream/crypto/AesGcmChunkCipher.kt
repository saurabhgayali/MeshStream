package com.meshstream.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM implementation of [ChunkCipher] backed by the Android Keystore.
 *
 * Keys are generated in the Keystore on first use and never leave the secure hardware.
 * Each chunk uses a unique random IV (96 bits) and has its identity bound as AAD.
 *
 * Ciphertext layout (all lengths in bytes):
 * ```
 * ┌────────┬──────────┬──────────────────┐
 * │ IV     │ GCM tag  │ ciphertext       │
 * │ 12 B   │ 16 B     │ variable         │
 * └────────┴──────────┴──────────────────┘
 * ```
 *
 * Note: GCM appends the tag to the end of the ciphertext in the JCE API, so in practice
 * the JCE output is `ciphertext ‖ tag`. This class stores `IV ‖ JCE_OUTPUT` so that
 * decrypt can recover both IV and the JCE ciphertext+tag blob.
 */
class AesGcmChunkCipher : ChunkCipher {

    private val secureRandom = SecureRandom()

    override suspend fun encrypt(
        plaintext: ByteArray,
        chunkId: UUID,
        sessionId: UUID,
        sequenceNumber: Long,
        keyHandle: String,
    ): ByteArray = withContext(Dispatchers.Default) {
        try {
            val key = getOrCreateKey(keyHandle)
            val iv = ByteArray(IV_SIZE_BYTES).also { secureRandom.nextBytes(it) }
            val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            cipher.updateAAD(buildAad(chunkId, sessionId, sequenceNumber))
            val jceOutput = cipher.doFinal(plaintext)
            // Prepend IV to JCE output (ciphertext ‖ tag)
            ByteBuffer.allocate(IV_SIZE_BYTES + jceOutput.size)
                .put(iv)
                .put(jceOutput)
                .array()
        } catch (e: Exception) {
            throw EncryptionException("AES-GCM encryption failed for chunk $chunkId", e)
        }
    }

    override suspend fun decrypt(
        ciphertext: ByteArray,
        chunkId: UUID,
        sessionId: UUID,
        sequenceNumber: Long,
        keyHandle: String,
    ): ByteArray = withContext(Dispatchers.Default) {
        try {
            require(ciphertext.size > IV_SIZE_BYTES) {
                "Ciphertext too short: ${ciphertext.size} bytes"
            }
            val key = getOrCreateKey(keyHandle)
            val iv = ciphertext.copyOfRange(0, IV_SIZE_BYTES)
            val jceInput = ciphertext.copyOfRange(IV_SIZE_BYTES, ciphertext.size)
            val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.updateAAD(buildAad(chunkId, sessionId, sequenceNumber))
            cipher.doFinal(jceInput)
        } catch (e: Exception) {
            throw DecryptionException("AES-GCM decryption failed for chunk $chunkId", e)
        }
    }

    /**
     * Returns the existing Keystore key for [alias], or generates a new AES-256 key.
     */
    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        val existing = keyStore.getKey(alias, null)
        if (existing is SecretKey) return existing

        val keyGenSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(false) // We supply our own IV
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .also { it.init(keyGenSpec) }
            .generateKey()
    }

    /**
     * Builds the Additional Authenticated Data (AAD) that binds the ciphertext to its
     * chunk identity. Changing any of these fields will cause GCM tag verification to fail.
     *
     * Layout: chunkId (16 B) ‖ sessionId (16 B) ‖ sequenceNumber (8 B big-endian)
     */
    private fun buildAad(chunkId: UUID, sessionId: UUID, sequenceNumber: Long): ByteArray =
        ByteBuffer.allocate(AAD_SIZE_BYTES)
            .putLong(chunkId.mostSignificantBits)
            .putLong(chunkId.leastSignificantBits)
            .putLong(sessionId.mostSignificantBits)
            .putLong(sessionId.leastSignificantBits)
            .putLong(sequenceNumber)
            .array()

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val IV_SIZE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val AAD_SIZE_BYTES = 40 // 16 + 16 + 8
    }
}
