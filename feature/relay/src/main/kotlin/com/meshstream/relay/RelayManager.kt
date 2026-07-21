package com.meshstream.relay

import com.meshstream.core.event.MeshEvent
import com.meshstream.core.model.NodeInfo
import com.meshstream.core.model.VideoChunk
import com.meshstream.core.usecase.PropagateChunksUseCase
import com.meshstream.mesh.MeshTransport
import com.meshstream.mesh.PeerDiscoveryManager
import com.meshstream.storage.ChunkStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the store-and-forward relay loop.
 *
 * The relay manager:
 * 1. Listens for [MeshEvent.PeerDiscovered] events from [PeerDiscoveryManager].
 * 2. When a peer is available, retrieves pending chunks from [ChunkStore].
 * 3. Forwards each chunk via the appropriate [MeshTransport].
 * 4. Handles [DeliveryReceipt] acknowledgements via [PropagateChunksUseCase].
 * 5. Re-queues chunks that failed to transfer.
 *
 * The relay loop also runs on a periodic timer to handle peers that were
 * available but had no pending chunks at discovery time.
 *
 * @param propagateChunksUseCase Domain use case for chunk status management.
 * @param chunkStore Storage layer for chunk payloads.
 * @param peerDiscoveryManager Provides the stream of discovered peers.
 * @param transports Available transport implementations (Hilt-injected set).
 * @param eventBus Shared domain event bus.
 * @param scope Coroutine scope for background work.
 */
@Singleton
class RelayManager @Inject constructor(
    private val propagateChunksUseCase: PropagateChunksUseCase,
    private val chunkStore: ChunkStore,
    private val peerDiscoveryManager: PeerDiscoveryManager,
    private val transports: Set<@JvmSuppressWildcards MeshTransport>,
    private val eventBus: MutableSharedFlow<MeshEvent>,
    private val scope: CoroutineScope,
) {
    /** Currently known reachable peers. */
    private val knownPeers = mutableMapOf<String, NodeInfo>()

    /**
     * Starts the relay manager:
     * - Subscribes to peer discovery events.
     * - Starts the periodic propagation loop.
     */
    fun start() {
        observePeerEvents()
        startPeriodicLoop()
        peerDiscoveryManager.startDiscovery()
    }

    // ──────────────────────────── Internal ────────────────────────────────

    private fun observePeerEvents() {
        eventBus
            .onEach { event ->
                when (event) {
                    is MeshEvent.PeerDiscovered -> {
                        knownPeers[event.node.deviceId] = event.node
                        // Immediately attempt to forward pending chunks to this peer.
                        scope.launch { forwardPendingChunks(event.node) }
                    }
                    is MeshEvent.PeerLost -> knownPeers.remove(event.deviceId)
                    else -> Unit
                }
            }
            .launchIn(scope)
    }

    private fun startPeriodicLoop() {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                val peers = knownPeers.values.toList()
                peers.forEach { peer -> forwardPendingChunks(peer) }
            }
        }
    }

    /**
     * Attempts to forward all pending chunks to [peer] using the best available transport.
     */
    private suspend fun forwardPendingChunks(peer: NodeInfo) {
        val transport = selectTransport(peer) ?: return
        val chunks = propagateChunksUseCase.nextChunksToForward()

        for (chunk in chunks) {
            forwardChunk(chunk, peer, transport)
        }
    }

    private suspend fun forwardChunk(
        chunk: VideoChunk,
        peer: NodeInfo,
        transport: MeshTransport,
    ) {
        val payload = chunkStore.getPayload(chunk.id) ?: run {
            // Payload file missing; mark as failed and skip.
            propagateChunksUseCase.onTransferFailed(chunk.id)
            return
        }

        propagateChunksUseCase.markInFlight(chunk.id)

        val receipt = runCatching {
            transport.sendChunk(chunk, peer.deviceId, payload)
        }.getOrNull()

        if (receipt != null) {
            propagateChunksUseCase.onReceiptVerified(receipt)
            eventBus.emit(MeshEvent.ChunkDelivered(chunk.id, receipt))
        } else {
            propagateChunksUseCase.onTransferFailed(chunk.id)
            eventBus.emit(MeshEvent.ChunkTransferFailed(chunk.id, "No receipt from ${peer.deviceId}"))
        }
    }

    /**
     * Picks the best available [MeshTransport] for communicating with [peer].
     * Prefers a transport whose [MeshTransport.transportType] matches the peer's
     * last-known transport; falls back to any available transport.
     *
     * Returns null if no transport is available.
     */
    private fun selectTransport(peer: NodeInfo): MeshTransport? =
        transports.firstOrNull { it.isAvailable && it.transportType == peer.transport }
            ?: transports.firstOrNull { it.isAvailable }

    companion object {
        /** Interval between periodic propagation attempts when no new peers are discovered. */
        const val POLL_INTERVAL_MS: Long = 60_000L
    }
}
