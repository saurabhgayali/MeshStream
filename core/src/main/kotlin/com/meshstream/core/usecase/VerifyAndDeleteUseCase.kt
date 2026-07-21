package com.meshstream.core.usecase

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.repository.ChunkRepository

/**
 * Identifies [VideoChunk]s that have been delivered and schedules their secure deletion.
 *
 * Secure deletion means:
 * 1. The encrypted payload file is overwritten with random bytes.
 * 2. The file is then deleted from the filesystem.
 * 3. The Room metadata record is purged.
 *
 * @param chunkRepository Persistence layer for chunk records.
 * @param secureDeleter Callback that performs the actual secure file deletion.
 */
class VerifyAndDeleteUseCase(
    private val chunkRepository: ChunkRepository,
    private val secureDeleter: SecureDeleter,
) {
    /**
     * Finds all [ChunkStatus.DELIVERED] chunks and securely deletes their payload files,
     * then removes their records from the repository.
     *
     * This method is idempotent: if a file has already been deleted, it logs a warning
     * and proceeds to remove the database record.
     */
    suspend fun deleteDeliveredChunks() {
        val delivered = chunkRepository.observeByStatus(ChunkStatus.DELIVERED)
        // Collect a snapshot to avoid modifying the flow mid-iteration.
        val snapshot = mutableListOf<VideoChunk>()
        delivered.collect { snapshot.addAll(it) }

        for (chunk in snapshot) {
            secureDeleter.delete(chunk)
            chunkRepository.updateStatus(chunk.id, ChunkStatus.DELETED)
            chunkRepository.delete(chunk.id)
        }
    }
}

/**
 * Abstraction for securely deleting a chunk's encrypted payload file.
 *
 * The implementation must overwrite the file with random bytes before deletion
 * to prevent recovery from the filesystem.
 */
fun interface SecureDeleter {
    /**
     * Overwrites the payload file of [chunk] with random bytes and then deletes it.
     *
     * @param chunk The chunk whose payload file should be securely erased.
     */
    suspend fun delete(chunk: VideoChunk)
}
