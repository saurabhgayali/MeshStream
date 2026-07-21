package com.meshstream.app.di

import com.meshstream.core.event.MeshEvent
import com.meshstream.core.repository.ChunkRepository
import com.meshstream.core.repository.NodeRepository
import com.meshstream.core.repository.SessionRepository
import com.meshstream.core.usecase.PropagateChunksUseCase
import com.meshstream.core.usecase.RecordAndChunkUseCase
import com.meshstream.crypto.AesGcmChunkCipher
import com.meshstream.crypto.ChunkCipher
import com.meshstream.storage.ChunkStore
import com.meshstream.storage.InMemoryNodeRepository
import com.meshstream.storage.RoomChunkStore
import com.meshstream.storage.StorageChunkRepository
import com.meshstream.storage.StorageSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Singleton

/**
 * Hilt module that binds interfaces to their implementations and provides
 * application-scoped singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindChunkCipher(impl: AesGcmChunkCipher): ChunkCipher

    @Binds
    @Singleton
    abstract fun bindChunkStore(impl: RoomChunkStore): ChunkStore

    @Binds
    @Singleton
    abstract fun bindChunkRepository(impl: StorageChunkRepository): ChunkRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: StorageSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindNodeRepository(impl: InMemoryNodeRepository): NodeRepository

    companion object {

        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob())

        @Provides
        @Singleton
        fun provideMeshEventBus(): MutableSharedFlow<MeshEvent> =
            MutableSharedFlow(replay = 0, extraBufferCapacity = 128)

        @Provides
        @Singleton
        fun provideRecordAndChunkUseCase(
            chunkRepository: ChunkRepository,
            sessionRepository: SessionRepository,
        ): RecordAndChunkUseCase = RecordAndChunkUseCase(chunkRepository, sessionRepository)

        @Provides
        @Singleton
        fun providePropagateChunksUseCase(
            chunkRepository: ChunkRepository,
        ): PropagateChunksUseCase = PropagateChunksUseCase(chunkRepository)
    }
}
