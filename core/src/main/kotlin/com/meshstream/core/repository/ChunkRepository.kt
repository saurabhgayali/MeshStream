package com.meshstream.core.repository

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.VideoChunk
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Provides access to stored [VideoChunk] records and their associated encrypted files.
 *
 * Implementations are responsible for persisting both the chunk metadata (e.g., via Room)
 * and the encrypted payload file (e.g., in `filesDir`). The two must always be kept in sync:
 * a metadata record without a payload file (or vice versa) is an error condition.
 */
interface ChunkRepository {

    /**
     * Returns a [Flow] that emits the current list of all chunks, re-emitting whenever
     * any chunk is inserted, updated, or deleted.
     */
    fun observeAll(): Flow<List<VideoChunk>>

    /**
     * Returns a [Flow] that emits the current list of chunks matching [status].
     */
    fun observeByStatus(status: ChunkStatus): Flow<List<VideoChunk>>

    /**
     * Returns the chunk with the given [id], or null if not found.
     */
    suspend fun findById(id: UUID): VideoChunk?

    /**
     * Inserts a new chunk record. The encrypted payload file must already exist at
     * the path derived from [VideoChunk.id] before calling this method.
     *
     * @throws IllegalStateException if a chunk with the same [VideoChunk.id] already exists.
     */
    suspend fun insert(chunk: VideoChunk)

    /**
     * Updates the [VideoChunk.status] of an existing chunk.
     *
     * @throws NoSuchElementException if no chunk with [id] exists.
     */
    suspend fun updateStatus(id: UUID, status: ChunkStatus)

    /**
     * Increments the [VideoChunk.hops] counter for the chunk with the given [id].
     *
     * @throws NoSuchElementException if no chunk with [id] exists.
     */
    suspend fun incrementHops(id: UUID)

    /**
     * Permanently removes the chunk record from the repository.
     * The caller is responsible for securely deleting the payload file before invoking this.
     */
    suspend fun delete(id: UUID)

    /**
     * Returns the total storage used by all chunk payload files, in bytes.
     */
    suspend fun totalStorageUsedBytes(): Long

    /**
     * Returns all chunks in [ChunkStatus.PENDING] or [ChunkStatus.QUEUED] states,
     * ordered by [VideoChunk.sequenceNumber] ascending (oldest first).
     */
    suspend fun pendingChunks(): List<VideoChunk>
}
