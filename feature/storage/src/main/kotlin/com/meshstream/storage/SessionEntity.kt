package com.meshstream.storage

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.meshstream.core.model.RecordingSession
import com.meshstream.core.model.VideoResolution
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/** Room entity mirroring [RecordingSession]. */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "started_at_ms")
    val startedAtMs: Long,

    @ColumnInfo(name = "ended_at_ms")
    val endedAtMs: Long?,

    @ColumnInfo(name = "total_chunks")
    val totalChunks: Int,

    @ColumnInfo(name = "resolution")
    val resolution: String,

    @ColumnInfo(name = "frame_rate")
    val frameRate: Int,

    @ColumnInfo(name = "chunk_duration_ms")
    val chunkDurationMs: Long,
) {
    fun toDomain() = RecordingSession(
        id = UUID.fromString(id),
        startedAt = Instant.ofEpochMilli(startedAtMs),
        endedAt = endedAtMs?.let { Instant.ofEpochMilli(it) },
        totalChunks = totalChunks,
        resolution = VideoResolution.valueOf(resolution),
        frameRate = frameRate,
        chunkDurationMs = chunkDurationMs,
    )

    companion object {
        fun fromDomain(session: RecordingSession) = SessionEntity(
            id = session.id.toString(),
            startedAtMs = session.startedAt.toEpochMilli(),
            endedAtMs = session.endedAt?.toEpochMilli(),
            totalChunks = session.totalChunks,
            resolution = session.resolution.name,
            frameRate = session.frameRate,
            chunkDurationMs = session.chunkDurationMs,
        )
    }
}

/** Room DAO for [SessionEntity] operations. */
@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY started_at_ms DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE ended_at_ms IS NULL LIMIT 1")
    suspend fun findActive(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SessionEntity)

    @Update
    suspend fun update(entity: SessionEntity)
}
