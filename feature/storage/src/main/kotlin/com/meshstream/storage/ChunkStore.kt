package com.meshstream.storage

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.VideoChunk
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/**
 * Unified interface for storing and retrieving encrypted chunk payloads and metadata.
 *
 * The storage layer has two responsibilities:
 * 1. **Metadata** — a Room database record for each [VideoChunk], supporting queries
 *    by status, session, and sequence number.
 * 2. **Payload** — the encrypted byte stream stored as a file in internal app storage
 *    (`context.filesDir/chunks/<chunkId>`).
 *
 * The two are always kept in sync: inserting a chunk stores both; deleting removes both.
 */
interface ChunkStore {

    /**
     * Persists [chunk] metadata and writes [encryptedPayload] to the chunk file.
     *
     * @throws IllegalStateException if a chunk with [VideoChunk.id] already exists.
     */
    suspend fun put(chunk: VideoChunk, encryptedPayload: ByteArray)

    /**
     * Reads the encrypted payload for [chunkId].
     *
     * @return The raw encrypted bytes, or null if the chunk is not found.
     */
    suspend fun getPayload(chunkId: UUID): ByteArray?

    /**
     * Returns the [VideoChunk] metadata for [chunkId], or null if not found.
     */
    suspend fun getMetadata(chunkId: UUID): VideoChunk?

    /**
     * Observes all chunks, ordered by sequence number ascending within each session.
     */
    fun observeAll(): Flow<List<VideoChunk>>

    /**
     * Observes all chunks matching [status].
     */
    fun observeByStatus(status: ChunkStatus): Flow<List<VideoChunk>>

    /**
     * Updates the status of the chunk identified by [chunkId].
     *
     * @throws NoSuchElementException if no chunk with [chunkId] exists.
     */
    suspend fun updateStatus(chunkId: UUID, status: ChunkStatus)

    /**
     * Securely overwrites the payload file with random bytes, deletes it from the
     * filesystem, and removes the metadata record from the database.
     *
     * This method is idempotent: if the file has already been deleted, it proceeds
     * to remove the database record.
     *
     * @throws NoSuchElementException if no chunk with [chunkId] exists.
     */
    suspend fun secureDelete(chunkId: UUID)

    /**
     * Returns the on-disk path of the payload file for [chunkId].
     * The file may or may not exist (check existence before reading).
     */
    fun payloadFile(chunkId: UUID): File

    /**
     * Returns the total size in bytes of all chunk payload files currently on disk.
     */
    suspend fun totalStorageUsedBytes(): Long

    /**
     * Returns all chunks with [ChunkStatus.PENDING] or [ChunkStatus.QUEUED] status,
     * ordered by sequence number ascending.
     */
    suspend fun pendingChunks(): List<VideoChunk>
}
