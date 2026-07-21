package com.meshstream.core.usecase

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.RecordingSession
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.repository.ChunkRepository
import com.meshstream.core.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RecordAndChunkUseCaseTest {

    private lateinit var chunkRepository: ChunkRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: RecordAndChunkUseCase

    @Before
    fun setUp() {
        chunkRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        useCase = RecordAndChunkUseCase(chunkRepository, sessionRepository)
    }

    // ──────────────────────────── startSession ────────────────────────────

    @Test
    fun `startSession inserts session when no active session exists`() = runTest {
        coEvery { sessionRepository.findActive() } returns null
        val session = RecordingSession()

        useCase.startSession(session)

        coVerify(exactly = 1) { sessionRepository.insert(session) }
    }

    @Test(expected = IllegalStateException::class)
    fun `startSession throws when an active session already exists`() = runTest {
        val active = RecordingSession()
        coEvery { sessionRepository.findActive() } returns active

        useCase.startSession(RecordingSession())
    }

    // ──────────────────────────── onChunkProduced ─────────────────────────

    @Test
    fun `onChunkProduced stores chunk with PENDING status`() = runTest {
        val sessionId = UUID.randomUUID()
        val session = RecordingSession(id = sessionId)
        val chunk = makeChunk(sessionId = sessionId)

        coEvery { sessionRepository.findById(sessionId) } returns session

        useCase.onChunkProduced(chunk)

        coVerify(exactly = 1) {
            chunkRepository.insert(match { it.status == ChunkStatus.PENDING && it.id == chunk.id })
        }
    }

    @Test
    fun `onChunkProduced increments session totalChunks`() = runTest {
        val sessionId = UUID.randomUUID()
        val session = RecordingSession(id = sessionId, totalChunks = 5)
        val chunk = makeChunk(sessionId = sessionId)

        coEvery { sessionRepository.findById(sessionId) } returns session

        useCase.onChunkProduced(chunk)

        coVerify(exactly = 1) {
            sessionRepository.update(match { it.totalChunks == 6 })
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `onChunkProduced throws when session not found`() = runTest {
        val chunk = makeChunk()
        coEvery { sessionRepository.findById(any()) } returns null

        useCase.onChunkProduced(chunk)
    }

    @Test(expected = IllegalStateException::class)
    fun `onChunkProduced throws when session is already finished`() = runTest {
        val sessionId = UUID.randomUUID()
        val finishedSession = RecordingSession(id = sessionId, endedAt = Instant.now())
        val chunk = makeChunk(sessionId = sessionId)

        coEvery { sessionRepository.findById(sessionId) } returns finishedSession

        useCase.onChunkProduced(chunk)
    }

    // ──────────────────────────── endSession ──────────────────────────────

    @Test
    fun `endSession sets endedAt on the session`() = runTest {
        val sessionId = UUID.randomUUID()
        val session = RecordingSession(id = sessionId)

        coEvery { sessionRepository.findById(sessionId) } returns session

        useCase.endSession(sessionId)

        coVerify(exactly = 1) {
            sessionRepository.update(match { it.endedAt != null })
        }
    }

    @Test(expected = NoSuchElementException::class)
    fun `endSession throws when session not found`() = runTest {
        coEvery { sessionRepository.findById(any()) } returns null

        useCase.endSession(UUID.randomUUID())
    }

    // ──────────────────────────── Helpers ─────────────────────────────────

    private fun makeChunk(sessionId: UUID = UUID.randomUUID()) = VideoChunk(
        sessionId = sessionId,
        sequenceNumber = 0L,
        durationMs = 30_000L,
        sizeBytes = 1024L,
        sha256Digest = ByteArray(32),
        keyHandle = "test-key",
        originDeviceId = "device-1",
    )
}
