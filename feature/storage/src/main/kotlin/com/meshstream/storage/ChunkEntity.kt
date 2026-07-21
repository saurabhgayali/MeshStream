package com.meshstream.storage

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.VideoChunk
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/**
 * Room entity that mirrors [VideoChunk], adapted for SQLite storage.
 *
 * UUIDs are stored as TEXT (canonical UUID string form).
 * [Instant]s are stored as epoch-millisecond LONGs.
 * [ByteArray]s (sha256Digest) are stored as BLOB.
 */
@Entity(tableName = "chunks")
data class ChunkEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "sha256_digest", typeAffinity = androidx.room.ColumnInfo.BLOB)
    val sha256Digest: ByteArray,

    @ColumnInfo(name = "key_handle")
    val keyHandle: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "hops")
    val hops: Int,

    @ColumnInfo(name = "origin_device_id")
    val originDeviceId: String,
) {
    fun toDomain(): VideoChunk = VideoChunk(
        id = UUID.fromString(id),
        sessionId = UUID.fromString(sessionId),
        sequenceNumber = sequenceNumber,
        durationMs = durationMs,
        createdAt = Instant.ofEpochMilli(createdAtMs),
        sizeBytes = sizeBytes,
        sha256Digest = sha256Digest,
        keyHandle = keyHandle,
        status = ChunkStatus.valueOf(status),
        hops = hops,
        originDeviceId = originDeviceId,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkEntity) return false
        return id == other.id && sha256Digest.contentEquals(other.sha256Digest)
    }

    override fun hashCode(): Int = 31 * id.hashCode() + sha256Digest.contentHashCode()

    companion object {
        fun fromDomain(chunk: VideoChunk) = ChunkEntity(
            id = chunk.id.toString(),
            sessionId = chunk.sessionId.toString(),
            sequenceNumber = chunk.sequenceNumber,
            durationMs = chunk.durationMs,
            createdAtMs = chunk.createdAt.toEpochMilli(),
            sizeBytes = chunk.sizeBytes,
            sha256Digest = chunk.sha256Digest,
            keyHandle = chunk.keyHandle,
            status = chunk.status.name,
            hops = chunk.hops,
            originDeviceId = chunk.originDeviceId,
        )
    }
}

/** Room DAO for [ChunkEntity] operations. */
@Dao
interface ChunkDao {

    @Query("SELECT * FROM chunks ORDER BY sequence_number ASC")
    fun observeAll(): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE status = :status ORDER BY sequence_number ASC")
    fun observeByStatus(status: String): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ChunkEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ChunkEntity)

    @Update
    suspend fun update(entity: ChunkEntity)

    @Query("UPDATE chunks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE chunks SET hops = hops + 1 WHERE id = :id")
    suspend fun incrementHops(id: String)

    @Query("DELETE FROM chunks WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT * FROM chunks WHERE status IN ('PENDING','QUEUED') ORDER BY sequence_number ASC",
    )
    suspend fun pendingChunks(): List<ChunkEntity>

    @Query("SELECT COALESCE(SUM(size_bytes), 0) FROM chunks")
    suspend fun totalSizeBytes(): Long
}
