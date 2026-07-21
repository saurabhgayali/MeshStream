package com.meshstream.app

/**
 * Temporary JVM entrypoint used to validate the initial MeshStream module structure
 * before Android-specific build tooling is available in this environment.
 */
import com.meshstream.core.domain.ChunkRecord
import com.meshstream.core.domain.ChunkRepository
import com.meshstream.core.domain.ChunkStatus
import com.meshstream.core.domain.ChunkTransferOffer
import com.meshstream.core.domain.ChunkTransferRequest
import com.meshstream.core.domain.PeerNode
import com.meshstream.core.domain.RecordingSession
import com.meshstream.core.domain.SessionRepository
import com.meshstream.core.domain.StorageFootprintCalculator
import com.meshstream.core.domain.TransportCapability
import com.meshstream.crypto.SessionKeyManager
import com.meshstream.mesh.ChunkTransportCoordinator
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

    transportCoordinator.registerPeer(
        PeerNode(
            id = "relay-1",
            name = "relay-1",
            endpoint = "mesh://relay-1",
            capabilities = setOf(TransportCapability.CHUNK_TRANSFER, TransportCapability.RELAY_FORWARDING)
        )
    )
    val transferOffer = ChunkTransferOffer(
        sessionId = session.id,
        chunkId = chunk.id,
        offeredBy = "relay-1",
        sizeBytes = 2_048L,
        checksum = "sha256:demo"
    )
    val transferRequest = ChunkTransferRequest(
        sessionId = session.id,
        chunkId = chunk.id,
        requestedBy = session.sourceDeviceId,
        requestedFrom = "relay-1"
    )
    val offerAck = transportCoordinator.offerChunk(transferOffer)
    val requestAck = transportCoordinator.requestChunk(transferRequest)

    val maxDurationSeconds = calculator.calculateMaxRecordingDuration(
        freeStorageBytes = 12_000_000_000L,
        videoBitrateBps = 12_000_000L
    )

    val status = listOf(
        "Sessions: ${sessionRepository.list().size}",
        "Chunks: ${chunkRepository.list().size}",
        "Max recording duration: ${maxDurationSeconds}s",
        "Session key: ${keyManager.createSessionKey(session.id)}",
        "Peers discovered: ${transportCoordinator.discoverPeers().size}",
        "Transfer offer accepted: ${offerAck.accepted}",
        "Transfer request accepted: ${requestAck.accepted}",
        "Relay queue: ${relayQueueManager.queueLimit("mesh")}"
    )

    println(status.joinToString(System.lineSeparator()))
}
