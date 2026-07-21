package com.meshstream.core.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a single encrypted, time-bounded slice of a video recording.
 *
 * A [VideoChunk] is the atomic unit of propagation in MeshStream. The recording device
 * slices the raw video into fixed-duration segments, encrypts each segment, and stores
 * the result as a [VideoChunk]. Relay nodes forward chunks opaquely — they never
 * decrypt the payload.
 *
 * @property id Globally unique identifier, generated on the originating device.
 * @property sessionId The [RecordingSession] this chunk belongs to.
 * @property sequenceNumber 0-based monotonic index within the session.
 * @property durationMs Actual playback duration of this chunk in milliseconds.
 * @property createdAt Wall-clock time when the chunk was finalized on the recording device.
 * @property sizeBytes Size of the encrypted payload in bytes.
 * @property sha256Digest SHA-256 digest of the encrypted payload for integrity verification.
 * @property keyHandle Android Keystore alias for the Data Encryption Key (DEK) of this chunk.
 * @property status Current propagation state of this chunk.
 * @property hops Number of relay hops this chunk has traversed so far.
 * @property originDeviceId Pseudonymous identifier of the device that created this chunk.
 */
data class VideoChunk(
    val id: UUID = UUID.randomUUID(),
    val sessionId: UUID,
    val sequenceNumber: Long,
    val durationMs: Long,
    val createdAt: Instant = Instant.now(),
    val sizeBytes: Long,
    val sha256Digest: ByteArray,
    val keyHandle: String,
    val status: ChunkStatus = ChunkStatus.PENDING,
    val hops: Int = 0,
    val originDeviceId: String,
) {
    /** Returns true if this chunk is eligible to be forwarded to a peer. */
    fun isForwardable(): Boolean = status == ChunkStatus.PENDING || status == ChunkStatus.QUEUED

    /** Returns true if the chunk has been verified as delivered and may be securely deleted. */
    fun isDeletable(): Boolean = status == ChunkStatus.DELIVERED

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoChunk) return false
        return id == other.id &&
            sessionId == other.sessionId &&
            sequenceNumber == other.sequenceNumber &&
            sha256Digest.contentEquals(other.sha256Digest)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + sha256Digest.contentHashCode()
        return result
    }
}
