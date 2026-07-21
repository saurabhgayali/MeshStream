package com.meshstream.mesh

import com.meshstream.core.event.MeshEvent
import com.meshstream.core.model.NodeInfo
import com.meshstream.core.model.Transport
import com.meshstream.core.repository.NodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates peer discovery across all available [MeshTransport] implementations.
 *
 * When a new peer is discovered on any transport, [PeerDiscoveryManager]:
 * 1. Upserts the [NodeInfo] into the [NodeRepository].
 * 2. Emits a [MeshEvent.PeerDiscovered] on the shared event bus.
 *
 * Stale node records (not seen in > [STALE_THRESHOLD_MS] ms) are pruned periodically
 * from the repository.
 *
 * @param transports The set of available transports (injected by Hilt based on capability).
 * @param nodeRepository Persistence for discovered peer records.
 * @param eventBus Shared event bus for domain events.
 * @param scope Coroutine scope for background discovery work.
 */
@Singleton
class PeerDiscoveryManager @Inject constructor(
    private val transports: Set<@JvmSuppressWildcards MeshTransport>,
    private val nodeRepository: NodeRepository,
    private val eventBus: MutableSharedFlow<MeshEvent>,
    private val scope: CoroutineScope,
) {
    private val _discoveredNodes = MutableSharedFlow<NodeInfo>(replay = 0, extraBufferCapacity = 64)

    /** Emits newly discovered or refreshed peer nodes from all transports. */
    val discoveredNodes: Flow<NodeInfo> = _discoveredNodes.asSharedFlow()

    /**
     * Starts continuous peer discovery on all available transports.
     * This function returns immediately; discovery runs in background coroutines.
     */
    fun startDiscovery() {
        val activeTransports = transports.filter { it.isAvailable }
        if (activeTransports.isEmpty()) return

        scope.launch {
            activeTransports
                .map { transport -> transport.discoverPeers() }
                .merge()
                .collect { node -> onPeerDiscovered(node) }
        }
    }

    /**
     * Stops all transport discovery and prunes stale node records.
     */
    suspend fun stopDiscovery() {
        transports.forEach { it.shutdown() }
        val staleThreshold = Instant.now().minusMillis(STALE_THRESHOLD_MS)
        nodeRepository.pruneStale(staleThreshold)
    }

    private suspend fun onPeerDiscovered(node: NodeInfo) {
        nodeRepository.upsert(node)
        _discoveredNodes.emit(node)
        eventBus.emit(MeshEvent.PeerDiscovered(node))
    }

    companion object {
        /** Nodes not seen in this many milliseconds are considered stale: 5 minutes. */
        const val STALE_THRESHOLD_MS: Long = 5 * 60 * 1_000L
    }
}
