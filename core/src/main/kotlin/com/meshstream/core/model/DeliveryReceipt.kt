package com.meshstream.core.model

import java.time.Instant
import java.util.UUID

/**
 * A cryptographically authenticated acknowledgement that a peer has successfully
 * received and stored a [VideoChunk].
 *
 * The HMAC is computed as:
 * ```
 * HMAC-SHA256(chunkId ‖ receiverId ‖ timestamp.toEpochMilli(), sharedSecret)
 * ```
 * where `sharedSecret` is derived from the ECDH exchange between the originating
 * device's private key and the receiver's public key.
 *
 * @property chunkId Identifier of the chunk being acknowledged.
 * @property receiverId Pseudonymous device ID of the node that received the chunk.
 * @property timestamp Wall-clock time on the receiver when the chunk was stored.
 * @property hmac HMAC-SHA256 authentication tag (32 bytes).
 */
data class DeliveryReceipt(
    val chunkId: UUID,
    val receiverId: String,
    val timestamp: Instant,
    val hmac: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeliveryReceipt) return false
        return chunkId == other.chunkId &&
            receiverId == other.receiverId &&
            timestamp == other.timestamp &&
            hmac.contentEquals(other.hmac)
    }

    override fun hashCode(): Int {
        var result = chunkId.hashCode()
        result = 31 * result + receiverId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + hmac.contentHashCode()
        return result
    }
}
