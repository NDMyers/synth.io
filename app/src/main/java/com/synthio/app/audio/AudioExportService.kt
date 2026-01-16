package com.synthio.app.audio

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.synthio.app.data.ExportDatabase
import com.synthio.app.data.ExportedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service for exporting audio from the looper to device storage.
 * Uses MediaStore API for Android 10+ and direct file access for older versions.
 */
class AudioExportService(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioExportService"
        private const val EXPORTS_DIR = "exports"
    }
    
    private val database by lazy { ExportDatabase.getDatabase(context) }
    private val dao by lazy { database.exportedFileDao() }
    
    /**
     * Export audio to internal app storage.
     * 
     * @param trackMask Bitmask of tracks to include
     * @param includeDrums Whether to include drums
     * @param quality "compressed" for AAC, "high_quality" for WAV
     * @param onProgress Callback for progress updates (0.0 to 1.0)
     * @return The ExportedFile record if successful, null if failed
     */
    suspend fun exportAudio(
        trackMask: Int,
        includeDrums: Boolean,
        quality: String,
        onProgress: suspend (Float) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            
            // Get audio buffer from native
            val buffer = SynthesizerEngine.looperGetMixedBuffer(trackMask)
            if (buffer == null || buffer.isEmpty()) {
                Log.e(TAG, "Failed to get audio buffer, no content")
                return@withContext null
            }
            
            onProgress(0.3f)
            
            // Create exports directory if needed
            val exportsDir = File(context.filesDir, EXPORTS_DIR)
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }
            
            // Generate filename
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val extension = if (quality == "compressed") "aac" else "wav"
            val filename = "SynthIO_Loop_$timestamp.$extension"
            val file = File(exportsDir, filename)
            
            onProgress(0.4f)
            
            // Encode and save to internal storage
            FileOutputStream(file).use { outputStream ->
                if (quality == "compressed") {
                    AacEncoder.encode(buffer, outputStream)
                } else {
                    WavEncoder.encode(buffer, outputStream)
                }
            }
            
            onProgress(0.9f)
            
            // Calculate metadata
            val durationMs = WavEncoder.calculateDurationMs(buffer.size)
            val fileSize = file.length()
            
            // Create database record
            val exportedFile = ExportedFile(
                filename = filename,
                filePath = file.absolutePath,
                trackMask = trackMask,
                includeDrums = includeDrums,
                quality = quality,
                durationMs = durationMs,
                fileSize = fileSize
            )
            
            val id = dao.insert(exportedFile)
            onProgress(1.0f)
            
            Log.i(TAG, "Export complete to internal storage: ${file.absolutePath}")
            return@withContext exportedFile.copy(id = id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            return@withContext null
        }
    }
    
    /**
     * Copy an internal export file to a user-selected URI (SAF).
     * 
     * @param exportedFile The export record containing the source file path
     * @param targetUri The URI selected by the user via CreateDocument
     * @return true if successful, false otherwise
     */
    suspend fun copyToUri(exportedFile: ExportedFile, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val internalFile = File(exportedFile.filePath)
            if (!internalFile.exists()) {
                Log.e(TAG, "Internal file not found: ${exportedFile.filePath}")
                return@withContext false
            }
            
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                internalFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            
            Log.i(TAG, "File copied to URI: $targetUri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to URI", e)
            false
        }
    }
    
    /**
     * Delete an exported file from internal storage and database.
     */
    suspend fun deleteExport(exportedFile: ExportedFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(exportedFile.filePath)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteById(exportedFile.id)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting export", e)
            false
        }
    }
    /**
     * Get all exports from the database.
     */
    fun getAllExports() = dao.getAllExports()

}
