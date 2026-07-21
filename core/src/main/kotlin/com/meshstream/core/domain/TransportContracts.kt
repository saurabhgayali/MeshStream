package com.meshstream.core.domain

import java.time.Instant

/**
 * Transport contracts for Milestone 2 chunk movement between nearby MeshStream devices.
 *
 * These types define the minimum discovery, transfer, retry, and acknowledgement flow needed
 * for a mesh-style transport layer while keeping the core module free of Android dependencies.
 */
data class PeerNode(
    val id: String,
    val name: String,
    val endpoint: String,
    val capabilities: Set<TransportCapability> = emptySet()
)

enum class TransportCapability {
    CHUNK_TRANSFER,
    RELAY_FORWARDING
}

data class ChunkTransferOffer(
    val sessionId: String,
    val chunkId: String,
    val offeredBy: String,
    val sizeBytes: Long,
    val checksum: String,
    val createdAt: Instant = Instant.now()
)

data class ChunkTransferRequest(
    val sessionId: String,
    val chunkId: String,
    val requestedBy: String,
    val requestedFrom: String,
    val retryCount: Int = 0,
    val createdAt: Instant = Instant.now()
)

data class ChunkTransferAck(
    val sessionId: String,
    val chunkId: String,
    val recipient: String,
    val accepted: Boolean,
    val reason: String? = null,
    val createdAt: Instant = Instant.now()
)

interface ChunkTransportService {
    fun discoverPeers(): List<PeerNode>
    fun offerChunk(offer: ChunkTransferOffer): ChunkTransferAck
    fun requestChunk(request: ChunkTransferRequest): ChunkTransferAck
    fun acknowledge(ack: ChunkTransferAck): ChunkTransferAck
}
