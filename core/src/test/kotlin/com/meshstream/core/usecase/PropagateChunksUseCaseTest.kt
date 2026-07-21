package com.meshstream.core.usecase

import com.meshstream.core.model.ChunkStatus
import com.meshstream.core.model.DeliveryReceipt
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.repository.ChunkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PropagateChunksUseCaseTest {

    private lateinit var chunkRepository: ChunkRepository
    private lateinit var useCase: PropagateChunksUseCase

    @Before
    fun setUp() {
        chunkRepository = mockk(relaxed = true)
        useCase = PropagateChunksUseCase(chunkRepository, maxBatchSize = 3)
    }

    @Test
    fun `nextChunksToForward returns up to maxBatchSize pending chunks`() = runTest {
        val chunks = List(5) { makeChunk(seq = it.toLong()) }
        coEvery { chunkRepository.pendingChunks() } returns chunks

        val result = useCase.nextChunksToForward()

        assertEquals(3, result.size)
        assertEquals(0L, result[0].sequenceNumber)
        assertEquals(1L, result[1].sequenceNumber)
        assertEquals(2L, result[2].sequenceNumber)
    }

    @Test
    fun `nextChunksToForward returns empty list when no pending chunks`() = runTest {
        coEvery { chunkRepository.pendingChunks() } returns emptyList()

        val result = useCase.nextChunksToForward()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `markInFlight updates chunk status to IN_FLIGHT`() = runTest {
        val id = UUID.randomUUID()

        useCase.markInFlight(id)

        coVerify(exactly = 1) { chunkRepository.updateStatus(id, ChunkStatus.IN_FLIGHT) }
    }

    @Test
    fun `onReceiptVerified marks chunk DELIVERED and increments hops`() = runTest {
        val chunkId = UUID.randomUUID()
        val receipt = DeliveryReceipt(
            chunkId = chunkId,
            receiverId = "peer-device",
            timestamp = Instant.now(),
            hmac = ByteArray(32),
        )

        useCase.onReceiptVerified(receipt)

        coVerify(exactly = 1) { chunkRepository.updateStatus(chunkId, ChunkStatus.DELIVERED) }
        coVerify(exactly = 1) { chunkRepository.incrementHops(chunkId) }
    }

    @Test
    fun `onTransferFailed re-queues the chunk after marking it FAILED`() = runTest {
        val id = UUID.randomUUID()

        useCase.onTransferFailed(id)

        coVerify(exactly = 1) { chunkRepository.updateStatus(id, ChunkStatus.FAILED) }
        coVerify(exactly = 1) { chunkRepository.updateStatus(id, ChunkStatus.QUEUED) }
    }

    @Test
    fun `DEFAULT_BATCH_SIZE is 10`() {
        assertEquals(10, PropagateChunksUseCase.DEFAULT_BATCH_SIZE)
    }

    // ──────────────────────────── Helpers ─────────────────────────────────

    private fun makeChunk(seq: Long = 0) = VideoChunk(
        sessionId = UUID.randomUUID(),
        sequenceNumber = seq,
        durationMs = 30_000L,
        sizeBytes = 1024L,
        sha256Digest = ByteArray(32),
        keyHandle = "key-$seq",
        originDeviceId = "device-1",
    )
}
