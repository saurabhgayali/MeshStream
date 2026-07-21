package com.meshstream.core.event

import com.meshstream.core.model.DeliveryReceipt
import com.meshstream.core.model.NodeInfo
import com.meshstream.core.model.VideoChunk
import java.util.UUID

/**
 * Sealed hierarchy of domain events emitted within MeshStream.
 *
 * Events flow from feature modules to the relay orchestration layer via a shared
 * [kotlinx.coroutines.flow.SharedFlow]. Consumers may filter to specific subtypes.
 */
sealed interface MeshEvent {

    /**
     * Emitted by [feature/recorder] when a new encrypted chunk is ready in local storage.
     */
    data class ChunkProduced(val chunk: VideoChunk) : MeshEvent

    /**
     * Emitted by [feature/relay] when a chunk has been successfully transferred to a peer
     * and a valid [DeliveryReceipt] has been verified.
     */
    data class ChunkDelivered(val chunkId: UUID, val receipt: DeliveryReceipt) : MeshEvent

    /**
     * Emitted by [feature/relay] when a transfer attempt has permanently failed after all
     * retries have been exhausted. The chunk is re-queued automatically.
     */
    data class ChunkTransferFailed(val chunkId: UUID, val reason: String) : MeshEvent

    /**
     * Emitted by [feature/mesh] when a new peer node is discovered via any transport.
     */
    data class PeerDiscovered(val node: NodeInfo) : MeshEvent

    /**
     * Emitted by [feature/mesh] when a previously discovered peer is no longer reachable.
     */
    data class PeerLost(val deviceId: String) : MeshEvent

    /**
     * Emitted when the device gains internet connectivity and promotes itself to INITIATOR.
     */
    data object ConnectivityGained : MeshEvent

    /**
     * Emitted when the device loses internet connectivity and demotes itself from INITIATOR.
     */
    data object ConnectivityLost : MeshEvent

    /**
     * Emitted by [feature/storage] when the storage usage exceeds a configured threshold.
     * @property usedBytes Current storage used by chunk files.
     * @property capBytes Configured storage cap.
     */
    data class StorageThresholdExceeded(val usedBytes: Long, val capBytes: Long) : MeshEvent
}
