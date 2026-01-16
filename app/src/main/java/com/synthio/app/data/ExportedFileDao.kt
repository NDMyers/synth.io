package com.synthio.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for exported files.
 * Provides CRUD operations for the exported_files table.
 */
@Dao
interface ExportedFileDao {
    
    /** Get all exported files ordered by creation date (newest first) */
    @Query("SELECT * FROM exported_files ORDER BY createdAt DESC")
    fun getAllExports(): Flow<List<ExportedFile>>
    
    /** Get a single export by ID */
    @Query("SELECT * FROM exported_files WHERE id = :id")
    suspend fun getExportById(id: Long): ExportedFile?
    
    /** Insert a new export record */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(export: ExportedFile): Long
    
    /** Delete an export record by ID */
    @Query("DELETE FROM exported_files WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /** Delete an export record by file path */
    @Query("DELETE FROM exported_files WHERE filePath = :filePath")
    suspend fun deleteByFilePath(filePath: String)
    
    /** Delete all exports */
    @Query("DELETE FROM exported_files")
    suspend fun deleteAll()
    
    /** Get count of exports */
    @Query("SELECT COUNT(*) FROM exported_files")
    suspend fun getExportCount(): Int
}
