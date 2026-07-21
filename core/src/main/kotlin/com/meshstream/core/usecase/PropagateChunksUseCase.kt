package com.meshstream.core.usecase

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.DeliveryReceipt
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.repository.ChunkRepository
import java.util.UUID

/**
 * Orchestrates the selection and status management of chunks eligible for propagation.
 *
 * The relay manager calls [nextChunksToForward] to obtain a batch of chunks ready for
 * transfer, marks them [ChunkStatus.IN_FLIGHT] via [markInFlight], and then calls
 * [onReceiptVerified] when a valid [DeliveryReceipt] arrives or [onTransferFailed]
 * on failure.
 *
 * @param chunkRepository Persistence layer for [VideoChunk] records.
 * @param maxBatchSize Maximum number of chunks to include in a single propagation batch.
 */
class PropagateChunksUseCase(
    private val chunkRepository: ChunkRepository,
    private val maxBatchSize: Int = DEFAULT_BATCH_SIZE,
) {
    /**
     * Returns up to [maxBatchSize] chunks that are ready to be forwarded.
     * Chunks are ordered by sequence number ascending (oldest first) to ensure ordered delivery.
     */
    suspend fun nextChunksToForward(): List<VideoChunk> =
        chunkRepository.pendingChunks().take(maxBatchSize)

    /**
     * Transitions the chunk identified by [chunkId] to [ChunkStatus.IN_FLIGHT].
     * Called immediately before the transport layer begins sending.
     *
     * @throws NoSuchElementException if no chunk with [chunkId] exists.
     */
    suspend fun markInFlight(chunkId: UUID) {
        chunkRepository.updateStatus(chunkId, ChunkStatus.IN_FLIGHT)
    }

    /**
     * Handles a verified [DeliveryReceipt]:
     * - Updates the chunk status to [ChunkStatus.DELIVERED].
     * - Increments the hop counter.
     *
     * Secure deletion is scheduled by the storage module on receipt of the
     * [ChunkStatus.DELIVERED] status change (via Room observer).
     *
     * @param receipt A receipt whose HMAC has already been validated by [ReceiptVerifier].
     * @throws NoSuchElementException if no chunk with [receipt.chunkId] exists.
     */
    suspend fun onReceiptVerified(receipt: DeliveryReceipt) {
        chunkRepository.updateStatus(receipt.chunkId, ChunkStatus.DELIVERED)
        chunkRepository.incrementHops(receipt.chunkId)
    }

    /**
     * Handles a failed transfer attempt by reverting the chunk to [ChunkStatus.QUEUED]
     * so it can be retried.
     *
     * @param chunkId The chunk whose transfer failed.
     * @throws NoSuchElementException if no chunk with [chunkId] exists.
     */
    suspend fun onTransferFailed(chunkId: UUID) {
        chunkRepository.updateStatus(chunkId, ChunkStatus.FAILED)
        // Re-queue for retry; the relay manager drives the retry schedule.
        chunkRepository.updateStatus(chunkId, ChunkStatus.QUEUED)
    }

    companion object {
        /** Default maximum number of chunks included in one propagation batch. */
        const val DEFAULT_BATCH_SIZE: Int = 10
    }
}
