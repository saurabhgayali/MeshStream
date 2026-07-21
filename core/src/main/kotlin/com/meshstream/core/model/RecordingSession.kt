package com.meshstream.core.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a single continuous video recording session.
 *
 * A session spans from the moment the user starts recording to when they stop.
 * It groups all [VideoChunk]s produced during that period. If the app is killed
 * or the device reboots during recording, the session is automatically closed
 * and the chunks produced so far are preserved.
 *
 * @property id Globally unique identifier for this session.
 * @property startedAt When recording began.
 * @property endedAt When recording ended, or null if still in progress.
 * @property totalChunks Total number of chunks produced (updated incrementally).
 * @property resolution The video resolution used for this session.
 * @property frameRate The video frame rate used for this session.
 * @property chunkDurationMs The target duration of each chunk in milliseconds.
 */
data class RecordingSession(
    val id: UUID = UUID.randomUUID(),
    val startedAt: Instant = Instant.now(),
    val endedAt: Instant? = null,
    val totalChunks: Int = 0,
    val resolution: VideoResolution = VideoResolution.HD_720P,
    val frameRate: Int = 30,
    val chunkDurationMs: Long = DEFAULT_CHUNK_DURATION_MS,
) {
    val isActive: Boolean get() = endedAt == null

    companion object {
        /** Default chunk duration: 30 seconds. */
        const val DEFAULT_CHUNK_DURATION_MS: Long = 30_000L
    }
}

/**
 * Standard video resolutions supported by MeshStream.
 *
 * Lower resolutions produce smaller chunks and are preferred when storage or
 * bandwidth is constrained.
 */
enum class VideoResolution(val width: Int, val height: Int) {
    SD_480P(width = 854, height = 480),
    HD_720P(width = 1280, height = 720),
    FHD_1080P(width = 1920, height = 1080),
}
