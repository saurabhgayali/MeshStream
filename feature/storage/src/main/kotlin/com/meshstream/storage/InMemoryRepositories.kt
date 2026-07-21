
package com.meshstream.storage

import com.meshstream.core.domain.ChunkRecord
import com.meshstream.core.domain.ChunkRepository
import com.meshstream.core.domain.RecordingSession
import com.meshstream.core.domain.SessionRepository
import java.util.concurrent.ConcurrentHashMap

class InMemoryChunkRepository : ChunkRepository {
    private val chunks = ConcurrentHashMap<String, ChunkRecord>()

    override fun save(chunk: ChunkRecord): ChunkRecord {
        chunks[chunk.id] = chunk
        return chunk
    }

    override fun get(id: String): ChunkRecord? = chunks[id]

    override fun list(): List<ChunkRecord> = chunks.values.toList()
}

class InMemorySessionRepository : SessionRepository {
    private val sessions = ConcurrentHashMap<String, RecordingSession>()

    override fun save(session: RecordingSession): RecordingSession {
        sessions[session.id] = session
        return session
    }

    override fun get(id: String): RecordingSession? = sessions[id]

    override fun list(): List<RecordingSession> = sessions.values.toList()
}
