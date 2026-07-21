package com.meshstream.core.repository

import com.meshstream.core.model.RecordingSession
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Provides access to [RecordingSession] records.
 */
interface SessionRepository {

    /**
     * Returns a [Flow] that emits all sessions, ordered by [RecordingSession.startedAt] descending.
     */
    fun observeAll(): Flow<List<RecordingSession>>

    /**
     * Returns the session with the given [id], or null if not found.
     */
    suspend fun findById(id: UUID): RecordingSession?

    /**
     * Returns the currently active (in-progress) session, or null if no recording is active.
     * There must be at most one active session at any time.
     */
    suspend fun findActive(): RecordingSession?

    /**
     * Inserts a new session.
     *
     * @throws IllegalStateException if there is already an active session.
     */
    suspend fun insert(session: RecordingSession)

    /**
     * Updates an existing session. Used to set [RecordingSession.endedAt] and update
     * [RecordingSession.totalChunks] incrementally.
     *
     * @throws NoSuchElementException if no session with [RecordingSession.id] exists.
     */
    suspend fun update(session: RecordingSession)
}
