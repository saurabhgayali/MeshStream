
package com.meshstream.core.domain

import java.time.Instant

data class RecordingSession(
    val id: String,
    val sourceDeviceId: String,
    val masterRecordingPath: String,
    val watchFolderPath: String,
    val createdAt: Instant
)

data class ChunkRecord(
    val id: String,
    val sessionId: String,
    val partNumber: Int,
    val chunkPath: String,
    val status: ChunkStatus,
    val createdAt: Instant
)

enum class ChunkStatus {
    PENDING,
    IN_TRANSIT,
    DELIVERED,
    FAILED
}
