
package com.meshstream.core.domain

interface ChunkRepository {
    fun save(chunk: ChunkRecord): ChunkRecord
    fun get(id: String): ChunkRecord?
    fun list(): List<ChunkRecord>
}

interface SessionRepository {
    fun save(session: RecordingSession): RecordingSession
    fun get(id: String): RecordingSession?
    fun list(): List<RecordingSession>
}
