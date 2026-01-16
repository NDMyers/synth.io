package com.synthio.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an exported audio file.
 * Stored in Room database for persistence across app sessions.
 */
@Entity(tableName = "exported_files")
data class ExportedFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Display name of the exported file (e.g., "Synth Loop 2026-01-15") */
    val filename: String,
    
    /** Absolute path to the file in internal storage */
    val filePath: String,
    
    /** Bitmask of which tracks were included (0-15 for tracks 0-3) */
    val trackMask: Int,
    
    /** Whether drums were included in the export */
    val includeDrums: Boolean,
    
    /** Quality: "compressed" or "high_quality" */
    val quality: String,
    
    /** Duration of the exported audio in milliseconds */
    val durationMs: Long,
    
    /** File size in bytes */
    val fileSize: Long,
    
    /** Timestamp when export was created */
    val createdAt: Long = System.currentTimeMillis()
)
