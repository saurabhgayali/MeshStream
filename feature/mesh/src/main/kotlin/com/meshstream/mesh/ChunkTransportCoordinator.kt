
package com.meshstream.mesh

import com.meshstream.core.domain.ChunkTransferAck
import com.meshstream.core.domain.ChunkTransferOffer
import com.meshstream.core.domain.ChunkTransferRequest
import com.meshstream.core.domain.ChunkTransportService
import com.meshstream.core.domain.PeerNode
import com.meshstream.core.domain.TransportCapability
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ChunkTransportCoordinator(
    private val localNodeId: String = "local-device"
) : ChunkTransportService {
    private val peers = ConcurrentHashMap<String, PeerNode>()

    fun registerPeer(peer: PeerNode) {
        peers[peer.id] = peer
    }

    override fun discoverPeers(): List<PeerNode> = peers.values.toList()

    override fun offerChunk(offer: ChunkTransferOffer): ChunkTransferAck {
        val peer = peers[offer.offeredBy]
        val accepted = peer != null && peer.capabilities.contains(TransportCapability.CHUNK_TRANSFER)

        return ChunkTransferAck(
            sessionId = offer.sessionId,
            chunkId = offer.chunkId,
            recipient = offer.offeredBy,
            accepted = accepted,
            reason = if (accepted) null else "No compatible peer registered for ${offer.offeredBy}",
            createdAt = Instant.now()
        )
    }

    override fun requestChunk(request: ChunkTransferRequest): ChunkTransferAck {
        val peer = peers[request.requestedFrom]
        val accepted = peer != null && peer.capabilities.contains(TransportCapability.CHUNK_TRANSFER)

        return ChunkTransferAck(
            sessionId = request.sessionId,
            chunkId = request.chunkId,
            recipient = request.requestedFrom,
            accepted = accepted,
            reason = if (accepted) null else "Unable to request chunk from ${request.requestedFrom}",
            createdAt = Instant.now()
        )
    }

    override fun acknowledge(ack: ChunkTransferAck): ChunkTransferAck =
        ack.copy(recipient = ack.recipient.ifBlank { localNodeId })

    @Deprecated("Temporary validation helper for the JVM entrypoint")
    fun describeRoute(targetDeviceName: String): String = "Route to $targetDeviceName"

    @Deprecated("Temporary validation helper for the JVM entrypoint")
    fun describeTransferPlan(request: ChunkTransferRequest): String =
        "Transfer ${request.chunkId} to ${request.requestedFrom} (attempt ${request.retryCount + 1})"
}
