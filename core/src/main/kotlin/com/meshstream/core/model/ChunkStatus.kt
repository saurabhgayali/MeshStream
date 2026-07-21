package com.meshstream.core.model

/**
 * Represents the propagation lifecycle of a [VideoChunk].
 *
 * State transitions:
 * ```
 * PENDING ──▶ QUEUED ──▶ IN_FLIGHT ──▶ DELIVERED ──▶ DELETED
 *                │                         │
 *                └─────── FAILED ──────────┘
 *                         (retried → QUEUED)
 * ```
 */
enum class ChunkStatus {
    /**
     * Chunk has been encrypted and stored locally. No peer has been found yet.
     * This is the initial state after encryption.
     */
    PENDING,

    /**
     * A suitable peer has been discovered and the chunk is queued for transfer.
     * Transition: [PENDING] → [QUEUED] when a peer is discovered.
     */
    QUEUED,

    /**
     * Transfer to a peer is currently in progress.
     * Transition: [QUEUED] → [IN_FLIGHT] when the transport layer begins sending.
     */
    IN_FLIGHT,

    /**
     * The chunk has been received and acknowledged by a peer (delivery receipt verified).
     * Transition: [IN_FLIGHT] → [DELIVERED] when a valid [DeliveryReceipt] is received.
     * Chunks in this state are eligible for secure deletion.
     */
    DELIVERED,

    /**
     * The chunk file has been securely overwritten and deleted. The Room record is
     * retained briefly for audit purposes, then also purged.
     * Transition: [DELIVERED] → [DELETED] after the secure deletion grace period.
     */
    DELETED,

    /**
     * A transfer attempt failed (transport error, timeout, or invalid receipt).
     * Transition: [IN_FLIGHT] → [FAILED]; subsequently retried → [QUEUED].
     */
    FAILED,
}
