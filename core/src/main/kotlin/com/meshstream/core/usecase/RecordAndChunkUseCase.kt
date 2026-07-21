package com.meshstream.core.usecase

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.RecordingSession
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.repository.ChunkRepository
import com.meshstream.core.repository.SessionRepository
import java.util.UUID

/**
 * Orchestrates the creation of a new [RecordingSession] and the storage of each
 * [VideoChunk] produced by the recorder.
 *
 * This use case is the bridge between the recording feature module and the core domain.
 * The recorder calls [onChunkProduced] each time a new encrypted chunk is ready.
 *
 * @param chunkRepository Persistence layer for [VideoChunk] records.
 * @param sessionRepository Persistence layer for [RecordingSession] records.
 */
class RecordAndChunkUseCase(
    private val chunkRepository: ChunkRepository,
    private val sessionRepository: SessionRepository,
) {
    /**
     * Starts a new recording session. Fails if a session is already active.
     *
     * @param session The new session to begin.
     * @throws IllegalStateException if there is already an active session.
     */
    suspend fun startSession(session: RecordingSession) {
        val active = sessionRepository.findActive()
        check(active == null) {
            "Cannot start a new session: session ${active?.id} is already active."
        }
        sessionRepository.insert(session)
    }

    /**
     * Records the arrival of a new encrypted chunk from the recorder.
     * The chunk is persisted with [ChunkStatus.PENDING] and the session's chunk count is updated.
     *
     * @param chunk The newly produced, encrypted chunk to persist.
     * @throws IllegalStateException if no active session exists for [VideoChunk.sessionId].
     */
    suspend fun onChunkProduced(chunk: VideoChunk) {
        val session = sessionRepository.findById(chunk.sessionId)
            ?: error("No session found for chunk ${chunk.id} (sessionId=${chunk.sessionId})")
        check(session.isActive) {
            "Cannot add chunk to finished session ${session.id}."
        }
        chunkRepository.insert(chunk.copy(status = ChunkStatus.PENDING))
        sessionRepository.update(session.copy(totalChunks = session.totalChunks + 1))
    }

    /**
     * Ends the recording session identified by [sessionId], recording the end time.
     *
     * @throws NoSuchElementException if no session with [sessionId] exists.
     */
    suspend fun endSession(sessionId: UUID) {
        val session = sessionRepository.findById(sessionId)
            ?: throw NoSuchElementException("Session $sessionId not found.")
        sessionRepository.update(session.copy(endedAt = java.time.Instant.now()))
    }
}
