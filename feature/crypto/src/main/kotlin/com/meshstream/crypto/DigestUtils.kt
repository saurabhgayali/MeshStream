package com.meshstream.crypto

import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Utility functions for computing SHA-256 digests and HMAC-SHA256 authentication tags.
 *
 * These are used to ensure chunk integrity and to authenticate [DeliveryReceipt]s.
 */
object DigestUtils {

    /**
     * Computes the SHA-256 digest of [data].
     *
     * @return A 32-byte SHA-256 digest.
     */
    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    /**
     * Computes an HMAC-SHA256 over the delivery receipt fields, binding them together
     * so that any tampering is detectable.
     *
     * The HMAC input is: chunkId (16 B) ‖ receiverId (UTF-8) ‖ timestamp millis (8 B)
     *
     * @param chunkId The chunk being acknowledged.
     * @param receiverId Pseudonymous device ID of the receiving node.
     * @param timestampMillis The receipt timestamp as epoch milliseconds.
     * @param sharedSecret The ECDH-derived shared secret between origin and receiver.
     * @return A 32-byte HMAC-SHA256 tag.
     */
    fun receiptHmac(
        chunkId: UUID,
        receiverId: String,
        timestampMillis: Long,
        sharedSecret: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(sharedSecret, "HmacSHA256"))

        // Feed each field separately to avoid length-extension ambiguity.
        mac.update(chunkId.mostSignificantBits.toByteArray())
        mac.update(chunkId.leastSignificantBits.toByteArray())
        mac.update(receiverId.toByteArray(Charsets.UTF_8))
        mac.update(timestampMillis.toByteArray())

        return mac.doFinal()
    }

    /**
     * Compares two byte arrays in constant time to prevent timing attacks.
     *
     * @return true if both arrays are equal, false otherwise.
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun Long.toByteArray(): ByteArray {
        val buffer = java.nio.ByteBuffer.allocate(Long.SIZE_BYTES)
        buffer.putLong(this)
        return buffer.array()
    }
}
