package com.meshstream.storage

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.repository.ChunkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ChunkRepository] implementation backed by Room.
 *
 * Handles metadata-only operations (no payload bytes).
 * Payload files are managed separately by [RoomChunkStore].
 */
@Singleton
class StorageChunkRepository @Inject constructor(
    private val chunkDao: ChunkDao,
) : ChunkRepository {

    override fun observeAll(): Flow<List<VideoChunk>> =
        chunkDao.observeAll().map { it.map(ChunkEntity::toDomain) }

    override fun observeByStatus(status: ChunkStatus): Flow<List<VideoChunk>> =
        chunkDao.observeByStatus(status.name).map { it.map(ChunkEntity::toDomain) }

    override suspend fun findById(id: UUID): VideoChunk? =
        chunkDao.findById(id.toString())?.toDomain()

    override suspend fun insert(chunk: VideoChunk) {
        chunkDao.insert(ChunkEntity.fromDomain(chunk))
    }

    override suspend fun updateStatus(id: UUID, status: ChunkStatus) {
        chunkDao.updateStatus(id.toString(), status.name)
    }

    override suspend fun incrementHops(id: UUID) {
        chunkDao.incrementHops(id.toString())
    }

    override suspend fun delete(id: UUID) {
        chunkDao.delete(id.toString())
    }

    override suspend fun totalStorageUsedBytes(): Long =
        chunkDao.totalSizeBytes()

    override suspend fun pendingChunks(): List<VideoChunk> =
        chunkDao.pendingChunks().map(ChunkEntity::toDomain)
}
