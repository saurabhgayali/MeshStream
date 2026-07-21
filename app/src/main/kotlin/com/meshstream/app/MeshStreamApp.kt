package com.meshstream.app

/**
 * Temporary JVM entrypoint used to validate the initial MeshStream module structure
 * before Android-specific build tooling is available in this environment.
 */
import com.meshstream.core.domain.ChunkRecord
import com.meshstream.core.domain.ChunkRepository
import com.meshstream.core.domain.ChunkStatus
import com.meshstream.core.domain.RecordingSession
import com.meshstream.core.domain.SessionRepository
import com.meshstream.core.domain.StorageFootprintCalculator
import com.meshstream.crypto.SessionKeyManager
import com.meshstream.mesh.ChunkTransportCoordinator
import com.meshstream.recorder.RecordingServiceContract
import com.meshstream.recorder.RecordingStatus
import com.meshstream.relay.RelayQueueManager
import com.meshstream.storage.InMemoryChunkRepository
import com.meshstream.storage.InMemorySessionRepository
import java.time.Instant

fun main() {
    val chunkRepository: ChunkRepository = InMemoryChunkRepository()
    val sessionRepository: SessionRepository = InMemorySessionRepository()
    val calculator = StorageFootprintCalculator()
    val keyManager = SessionKeyManager()
    val transportCoordinator = ChunkTransportCoordinator()
    val relayQueueManager = RelayQueueManager()

    val session = RecordingSession(
        id = "demo-session",
        sourceDeviceId = "demo-device",
        masterRecordingPath = "/tmp/master.mp4",
        watchFolderPath = "/tmp/watch",
        createdAt = Instant.now()
    )
    sessionRepository.save(session)

    val chunk = ChunkRecord(
        id = "demo-chunk",
        sessionId = session.id,
        partNumber = 1,
        chunkPath = "/tmp/watch/demo-chunk.mp4",
        status = ChunkStatus.PENDING,
        createdAt = Instant.now()
    )
    chunkRepository.save(chunk)

    val maxDurationSeconds = calculator.calculateMaxRecordingDuration(
        freeStorageBytes = 12_000_000_000L,
        videoBitrateBps = 12_000_000L
    )

    val status = listOf(
        "Sessions: ${sessionRepository.list().size}",
        "Chunks: ${chunkRepository.list().size}",
        "Max recording duration: ${maxDurationSeconds}s",
        "Session key: ${keyManager.createSessionKey(session.id)}",
        "Transport: ${transportCoordinator.describeRoute("relay-1")}",
        "Relay queue: ${relayQueueManager.queueLimit("mesh")}"
    )

    println(status.joinToString(System.lineSeparator()))
}
