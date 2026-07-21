package com.meshstream.app.di

import com.meshstream.storage.ChunkDao
import com.meshstream.storage.DatabaseProvider
import com.meshstream.storage.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides Room DAOs from the [DatabaseProvider]. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChunkDao(databaseProvider: DatabaseProvider): ChunkDao =
        databaseProvider.database.chunkDao()

    @Provides
    @Singleton
    fun provideSessionDao(databaseProvider: DatabaseProvider): SessionDao =
        databaseProvider.database.sessionDao()
}
