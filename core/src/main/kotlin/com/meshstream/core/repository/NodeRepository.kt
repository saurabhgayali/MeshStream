package com.meshstream.core.repository

import com.meshstream.core.model.NodeInfo
import com.meshstream.core.model.Transport
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Provides access to discovered peer [NodeInfo] records.
 *
 * Node records are ephemeral — they are updated on each discovery cycle and
 * automatically considered stale after a configurable TTL (default 5 minutes).
 */
interface NodeRepository {

    /**
     * Returns a [Flow] that emits the current list of recently-seen nodes,
     * re-emitting on any change.
     */
    fun observeAll(): Flow<List<NodeInfo>>

    /**
     * Upserts a [NodeInfo] record: inserts if [NodeInfo.deviceId] is new,
     * or updates [NodeInfo.lastSeen], [NodeInfo.rssi], and [NodeInfo.role] if it already exists.
     */
    suspend fun upsert(node: NodeInfo)

    /**
     * Removes all nodes that were last seen before [before].
     * Called periodically by the relay manager to expire stale records.
     */
    suspend fun pruneStale(before: Instant)

    /**
     * Returns all nodes discovered via the given [transport].
     */
    suspend fun findByTransport(transport: Transport): List<NodeInfo>
}
