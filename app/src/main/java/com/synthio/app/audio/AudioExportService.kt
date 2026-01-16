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
        private const val SAMPLE_RATE = 48000
        private const val CHANNELS = 2
    }
    
    private val database by lazy { ExportDatabase.getDatabase(context) }
    private val dao by lazy { database.exportedFileDao() }
    
    /**
     * Export audio with the given configuration.
     * 
     * @param trackMask Bitmask of tracks to include
     * @param includeDrums Whether to include drums (future feature)
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
            
            onProgress(0.2f)
            
            // Generate filename
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val extension = if (quality == "compressed") "aac" else "wav"
            val filename = "SynthIO_Loop_$timestamp.$extension"
            
            onProgress(0.3f)
            
            // Export to MediaStore
            val uri = saveToMediaStore(buffer, filename, quality == "compressed", onProgress)
            if (uri == null) {
                Log.e(TAG, "Failed to save to MediaStore")
                return@withContext null
            }
            
            onProgress(0.9f)
            
            // Calculate duration
            val durationMs = WavEncoder.calculateDurationMs(buffer.size)
            
            // Get file size
            val fileSize = getFileSize(uri)
            
            // Create database record
            val exportedFile = ExportedFile(
                filename = filename,
                uri = uri.toString(),
                trackMask = trackMask,
                includeDrums = includeDrums,
                quality = quality,
                durationMs = durationMs,
                fileSize = fileSize
            )
            
            val id = dao.insert(exportedFile)
            onProgress(1.0f)
            
            Log.i(TAG, "Export complete: $filename, size: $fileSize bytes, duration: ${durationMs}ms")
            return@withContext exportedFile.copy(id = id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            return@withContext null
        }
    }
    
    private suspend fun saveToMediaStore(
        buffer: FloatArray,
        filename: String,
        isCompressed: Boolean,
        onProgress: suspend (Float) -> Unit
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStoreQ(buffer, filename, isCompressed, onProgress)
        } else {
            saveToMediaStoreLegacy(buffer, filename, isCompressed, onProgress)
        }
    }
    
    private suspend fun saveToMediaStoreQ(
        buffer: FloatArray,
        filename: String,
        isCompressed: Boolean,
        onProgress: suspend (Float) -> Unit
    ): Uri? {
        val mimeType = if (isCompressed) "audio/aac" else "audio/wav"
        
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SynthIO")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                onProgress(0.4f)
                
                if (isCompressed) {
                    AacEncoder.encode(buffer, outputStream)
                } else {
                    WavEncoder.encode(buffer, outputStream)
                }
                
                onProgress(0.8f)
            }
            
            // Mark as complete
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to MediaStore", e)
            resolver.delete(uri, null, null)
            return null
        }
    }
    
    @Suppress("DEPRECATION")
    private suspend fun saveToMediaStoreLegacy(
        buffer: FloatArray,
        filename: String,
        isCompressed: Boolean,
        onProgress: suspend (Float) -> Unit
    ): Uri? {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val synthioDir = File(musicDir, "SynthIO")
        if (!synthioDir.exists()) {
            synthioDir.mkdirs()
        }
        
        val file = File(synthioDir, filename)
        
        try {
            FileOutputStream(file).use { outputStream ->
                onProgress(0.4f)
                
                if (isCompressed) {
                    AacEncoder.encode(buffer, outputStream)
                } else {
                    WavEncoder.encode(buffer, outputStream)
                }
                
                onProgress(0.8f)
            }
            
            // Add to MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DATA, file.absolutePath)
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.MIME_TYPE, if (isCompressed) "audio/aac" else "audio/wav")
            }
            
            return context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to file", e)
            return null
        }
    }
    
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Delete an exported file from both MediaStore and database.
     */
    suspend fun deleteExport(exportedFile: ExportedFile): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete from MediaStore
            val uri = Uri.parse(exportedFile.uri)
            context.contentResolver.delete(uri, null, null)
            
            // Delete from database
            dao.deleteById(exportedFile.id)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting export", e)
            false
        }
    }
}
