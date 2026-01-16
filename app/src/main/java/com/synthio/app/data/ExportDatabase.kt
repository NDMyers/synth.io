package com.synthio.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for synth.io app.
 * Stores persistent data like exported audio files.
 */
@Database(
    entities = [ExportedFile::class],
    version = 1,
    exportSchema = false
)
abstract class ExportDatabase : RoomDatabase() {
    
    abstract fun exportedFileDao(): ExportedFileDao
    
    companion object {
        @Volatile
        private var INSTANCE: ExportDatabase? = null
        
        fun getDatabase(context: Context): ExportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExportDatabase::class.java,
                    "synthio_exports.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
