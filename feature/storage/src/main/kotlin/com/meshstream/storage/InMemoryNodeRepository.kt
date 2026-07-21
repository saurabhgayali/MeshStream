package com.meshstream.storage

import com.meshstream.core.model.NodeInfo
import com.meshstream.core.model.Transport
import com.meshstream.core.repository.NodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory [NodeRepository] for Phase 1.
 *
 * Node records are held in a [MutableStateFlow] and are lost on process restart,
 * which is acceptable because peer discovery is re-run on each app start.
 * A Room-backed implementation will replace this in a later phase if persistence is needed.
 */
@Singleton
class InMemoryNodeRepository @Inject constructor() : NodeRepository {

    private val _nodes = MutableStateFlow<Map<String, NodeInfo>>(emptyMap())

    override fun observeAll(): Flow<List<NodeInfo>> =
        _nodes.asStateFlow().map { it.values.toList() }

    override suspend fun upsert(node: NodeInfo) {
        _nodes.update { current -> current + (node.deviceId to node) }
    }

    override suspend fun pruneStale(before: Instant) {
        _nodes.update { current ->
            current.filterValues { it.lastSeen.isAfter(before) }
        }
    }

    override suspend fun findByTransport(transport: Transport): List<NodeInfo> =
        _nodes.value.values.filter { it.transport == transport }
}
