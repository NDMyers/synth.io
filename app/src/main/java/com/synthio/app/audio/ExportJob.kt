package com.synthio.app.audio

import android.net.Uri
import java.util.UUID

/**
 * Quality level for audio export
 */
enum class ExportQuality {
    /** Compressed AAC (.m4a) - smaller file size, recommended for sharing */
    COMPRESSED,
    /** High quality WAV - larger file size, lossless audio */
    HIGH_QUALITY
}

/**
 * Status of an export job
 */
enum class ExportStatus {
    /** Waiting to start */
    PENDING,
    /** Mixing audio tracks */
    MIXING,
    /** Encoding to target format */
    ENCODING,
    /** Export completed successfully */
    COMPLETE,
    /** Export failed */
    FAILED
}

/**
 * Represents an audio export job
 */
data class ExportJob(
    val id: String = UUID.randomUUID().toString(),
    /** Bitmask of selected tracks (bit 0 = track 1, bit 1 = track 2, etc.) */
    val trackMask: Int,
    /** Whether to include drum machine audio in the mixdown */
    val includeDrums: Boolean,
    /** Export quality/format selection */
    val quality: ExportQuality,
    /** Current status of the export */
    val status: ExportStatus = ExportStatus.PENDING,
    /** Progress from 0.0 to 1.0 */
    val progress: Float = 0f,
    /** Absolute path to the exported file (when complete) */
    val outputFilePath: String? = null,
    /** Filename for display */
    val filename: String = "",
    /** Error message if failed */
    val errorMessage: String? = null,
    /** Timestamp when job was created */
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns a human-readable description of selected tracks
     */
    fun getTrackDescription(): String {
        val tracks = mutableListOf<Int>()
        for (i in 0 until 4) {
            if ((trackMask and (1 shl i)) != 0) {
                tracks.add(i + 1)
            }
        }
        return when {
            tracks.size == 4 -> "All Tracks"
            tracks.size == 1 -> "Track ${tracks[0]}"
            else -> "Tracks ${tracks.joinToString(", ")}"
        }
    }
    
    /**
     * Returns the file extension based on quality
     */
    fun getFileExtension(): String = when (quality) {
        ExportQuality.COMPRESSED -> "m4a"
        ExportQuality.HIGH_QUALITY -> "wav"
    }
    
    /**
     * Returns quality label for display
     */
    fun getQualityLabel(): String = when (quality) {
        ExportQuality.COMPRESSED -> "Standard"
        ExportQuality.HIGH_QUALITY -> "High Quality"
    }
}
