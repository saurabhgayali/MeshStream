package com.meshstream.storage

import com.meshstream.core.model.RecordingSession
import com.meshstream.core.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SessionRepository] implementation backed by Room.
 */
@Singleton
class StorageSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
) : SessionRepository {

    override fun observeAll(): Flow<List<RecordingSession>> =
        sessionDao.observeAll().map { it.map(SessionEntity::toDomain) }

    override suspend fun findById(id: UUID): RecordingSession? =
        sessionDao.findById(id.toString())?.toDomain()

    override suspend fun findActive(): RecordingSession? =
        sessionDao.findActive()?.toDomain()

    override suspend fun insert(session: RecordingSession) {
        val active = sessionDao.findActive()
        check(active == null) {
            "Cannot insert session ${session.id}: session ${active?.id} is already active."
        }
        sessionDao.insert(SessionEntity.fromDomain(session))
    }

    override suspend fun update(session: RecordingSession) {
        sessionDao.findById(session.id.toString())
            ?: throw NoSuchElementException("Session ${session.id} not found.")
        sessionDao.update(SessionEntity.fromDomain(session))
    }
}
