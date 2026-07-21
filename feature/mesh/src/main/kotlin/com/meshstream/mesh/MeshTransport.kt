package com.meshstream.mesh

import com.meshstream.core.model.DeliveryReceipt
import com.meshstream.core.model.NodeInfo
import com.meshstream.core.model.VideoChunk
import kotlinx.coroutines.flow.Flow

/**
 * Abstracts the physical layer transport used to discover peers and exchange chunks.
 *
 * Concrete implementations exist for Wi-Fi Aware, Wi-Fi Direct, and Bluetooth.
 * The [PeerDiscoveryManager] orchestrates all available transports and exposes a
 * unified [NodeInfo] stream.
 *
 * Implementations must be safe to call from any coroutine dispatcher.
 */
interface MeshTransport {

    /**
     * Returns a [Flow] that emits [NodeInfo] for each peer discovered (or rediscovered)
     * on this transport. The flow never completes unless the transport is shut down.
     *
     * Discovery runs continuously while the flow has active collectors.
     */
    fun discoverPeers(): Flow<NodeInfo>

    /**
     * Sends the encrypted payload of [chunk] to the peer identified by [targetDeviceId].
     *
     * @param chunk The chunk to send. Only the encrypted payload and metadata are transmitted.
     * @param targetDeviceId The pseudonymous device ID of the intended recipient.
     * @param payload The raw encrypted payload bytes.
     * @return The [DeliveryReceipt] returned by the peer, or null if the peer did not
     *   acknowledge within the timeout.
     * @throws TransportException if the transport layer reports an unrecoverable error.
     */
    suspend fun sendChunk(
        chunk: VideoChunk,
        targetDeviceId: String,
        payload: ByteArray,
    ): DeliveryReceipt?

    /**
     * Registers a handler to be called when this transport receives an incoming chunk
     * from a peer.
     *
     * @param handler Called with the incoming chunk and payload. Must return a
     *   [DeliveryReceipt] to acknowledge receipt.
     */
    fun onChunkReceived(handler: suspend (VideoChunk, ByteArray) -> DeliveryReceipt)

    /**
     * Returns true if this transport is currently available on the device
     * (hardware present and permission granted).
     */
    val isAvailable: Boolean

    /**
     * The [Transport] type this implementation handles.
     * Used by [com.meshstream.relay.RelayManager] to match transports to peer capabilities.
     */
    val transportType: com.meshstream.core.model.Transport

    /**
     * Shuts down this transport, releasing all resources. After calling [shutdown],
     * the transport must not be used again.
     */
    suspend fun shutdown()
}

/** Thrown when a transport-layer operation fails unrecoverably. */
class TransportException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
