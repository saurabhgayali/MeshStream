package com.meshstream.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Room database that stores [ChunkEntity] and [SessionEntity] records. */
@Database(
    entities = [ChunkEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MeshStreamDatabase : RoomDatabase() {
    abstract fun chunkDao(): ChunkDao
    abstract fun sessionDao(): SessionDao
}

/**
 * Provides a singleton [MeshStreamDatabase] instance.
 *
 * In production this uses the on-device SQLite file. In tests, use an in-memory variant.
 */
@Singleton
class DatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val database: MeshStreamDatabase by lazy {
        Room.databaseBuilder(
            context,
            MeshStreamDatabase::class.java,
            DATABASE_NAME,
        )
            // TODO: Replace with proper incremental migrations before v1.0 release (issue #1).
            // Destructive migration is only acceptable during early development.
            .fallbackToDestructiveMigration()
            .build()
    }

    companion object {
        private const val DATABASE_NAME = "meshstream.db"
    }
}
