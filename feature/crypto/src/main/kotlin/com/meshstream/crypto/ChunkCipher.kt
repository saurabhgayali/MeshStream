package com.meshstream.crypto

import java.util.UUID

/**
 * Provides encryption and decryption of video chunk payloads.
 *
 * Implementations use AES-256-GCM with keys managed by the Android Keystore.
 * The plaintext payload is a raw MP4 byte stream; the ciphertext is a self-contained
 * blob that includes the IV and the GCM authentication tag.
 *
 * **Ciphertext layout:**
 * ```
 * [ IV (12 bytes) | GCM tag (16 bytes) | ciphertext (variable) ]
 * ```
 *
 * The AAD bound to each chunk is:
 * ```
 * chunkId (16 bytes UUID) ‖ sequenceNumber (8 bytes big-endian) ‖ sessionId (16 bytes UUID)
 * ```
 */
interface ChunkCipher {

    /**
     * Encrypts [plaintext] and returns the ciphertext blob.
     *
     * A new 96-bit random IV is generated for every call. The resulting ciphertext includes
     * the IV prepended so that [decrypt] has everything it needs.
     *
     * @param plaintext The raw MP4 byte stream of the chunk.
     * @param chunkId Unique chunk identifier, bound as AAD.
     * @param sessionId Session identifier, bound as AAD.
     * @param sequenceNumber Chunk sequence number, bound as AAD.
     * @param keyHandle The Android Keystore alias for the chunk's DEK.
     * @return The ciphertext blob (IV ‖ tag ‖ ciphertext).
     * @throws EncryptionException if encryption fails for any reason.
     */
    suspend fun encrypt(
        plaintext: ByteArray,
        chunkId: UUID,
        sessionId: UUID,
        sequenceNumber: Long,
        keyHandle: String,
    ): ByteArray

    /**
     * Decrypts [ciphertext] and returns the original plaintext.
     *
     * @param ciphertext The ciphertext blob produced by [encrypt].
     * @param chunkId Must match the value used during [encrypt].
     * @param sessionId Must match the value used during [encrypt].
     * @param sequenceNumber Must match the value used during [encrypt].
     * @param keyHandle The Android Keystore alias for the chunk's DEK.
     * @return The original plaintext MP4 byte stream.
     * @throws DecryptionException if decryption fails or authentication tag verification fails.
     */
    suspend fun decrypt(
        ciphertext: ByteArray,
        chunkId: UUID,
        sessionId: UUID,
        sequenceNumber: Long,
        keyHandle: String,
    ): ByteArray
}

/** Thrown when chunk encryption fails. */
class EncryptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Thrown when chunk decryption or GCM tag verification fails. */
class DecryptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
