package com.meshstream.recorder

import com.meshstream.core.model.RecordingSession
import com.meshstream.core.model.VideoChunk
import kotlinx.coroutines.flow.Flow

/**
 * Records video and emits encrypted [VideoChunk]s as they are produced.
 *
 * The recording runs as a long-lived background operation, typically managed by a
 * foreground [android.app.Service]. The recorder slices the video stream into
 * fixed-duration segments using [android.media.MediaMuxer] and passes each raw segment
 * through the crypto layer before emitting it.
 *
 * Implementations must:
 * - Hold a `CAMERA` and `RECORD_AUDIO` permission at call time.
 * - Acquire a `PARTIAL_WAKE_LOCK` for the duration of the recording.
 * - Post a foreground service notification before calling [start].
 */
interface ChunkRecorder {

    /**
     * Starts recording for the given [session].
     *
     * Emits [VideoChunk]s via [chunks] as each fixed-duration segment is finalized and
     * encrypted. The flow remains open until [stop] is called or an unrecoverable error occurs.
     *
     * @param session The recording session parameters (resolution, frame rate, chunk duration).
     * @return A cold [Flow] that emits chunks as they become available.
     * @throws RecordingException if the camera or microphone cannot be opened.
     */
    fun start(session: RecordingSession): Flow<VideoChunk>

    /**
     * Stops recording and finalises any in-progress chunk.
     *
     * Calling [stop] causes the [Flow] returned by [start] to complete normally.
     * Any partial (sub-duration) chunk at the end of the session is finalized, encrypted,
     * and emitted before the flow completes.
     */
    suspend fun stop()

    /**
     * Returns true if a recording is currently in progress.
     */
    val isRecording: Boolean
}

/** Thrown when the recorder encounters an unrecoverable error. */
class RecordingException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
