package com.meshstream.storage

import android.content.Context
import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.VideoChunk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room + filesystem implementation of [ChunkStore].
 *
 * Metadata lives in the Room [ChunkDao]; encrypted payloads live in
 * `context.filesDir/chunks/<chunkId>`.
 *
 * Secure deletion overwrites the payload file with cryptographically random bytes
 * before removing it, making recovery harder even on non-encrypted filesystems.
 * Note: on Android 7+ with file-based encryption enabled, the underlying filesystem
 * already provides strong at-rest protection; the overwrite is an additional layer.
 */
@Singleton
class RoomChunkStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chunkDao: ChunkDao,
) : ChunkStore {

    private val chunksDir: File
        get() = File(context.filesDir, CHUNKS_DIR).also { it.mkdirs() }

    private val secureRandom = SecureRandom()

    override suspend fun put(chunk: VideoChunk, encryptedPayload: ByteArray) {
        withContext(Dispatchers.IO) {
            val file = payloadFile(chunk.id)
            file.writeBytes(encryptedPayload)
            chunkDao.insert(ChunkEntity.fromDomain(chunk))
        }
    }

    override suspend fun getPayload(chunkId: UUID): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = payloadFile(chunkId)
            if (file.exists()) file.readBytes() else null
        }

    override suspend fun getMetadata(chunkId: UUID): VideoChunk? =
        chunkDao.findById(chunkId.toString())?.toDomain()

    override fun observeAll(): Flow<List<VideoChunk>> =
        chunkDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeByStatus(status: ChunkStatus): Flow<List<VideoChunk>> =
        chunkDao.observeByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateStatus(chunkId: UUID, status: ChunkStatus) {
        chunkDao.updateStatus(chunkId.toString(), status.name)
    }

    override suspend fun secureDelete(chunkId: UUID) {
        withContext(Dispatchers.IO) {
            val file = payloadFile(chunkId)
            if (file.exists()) {
                overwriteWithRandom(file)
                file.delete()
            }
            chunkDao.delete(chunkId.toString())
        }
    }

    override fun payloadFile(chunkId: UUID): File =
        File(chunksDir, "$chunkId.chunk")

    override suspend fun totalStorageUsedBytes(): Long =
        withContext(Dispatchers.IO) {
            chunksDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        }

    override suspend fun pendingChunks(): List<VideoChunk> =
        chunkDao.pendingChunks().map { it.toDomain() }

    /**
     * Overwrites [file] with cryptographically random bytes of the same size.
     * This is a best-effort measure; the OS may still have the original data in
     * write buffers or journaling structures.
     */
    private fun overwriteWithRandom(file: File) {
        val randomBytes = ByteArray(file.length().coerceAtMost(MAX_OVERWRITE_BYTES).toInt())
        secureRandom.nextBytes(randomBytes)
        file.writeBytes(randomBytes)
    }

    companion object {
        private const val CHUNKS_DIR = "chunks"

        /** Overwrite at most this many bytes to bound memory usage on large files. */
        private const val MAX_OVERWRITE_BYTES = 16 * 1024 * 1024L // 16 MB
    }
}
