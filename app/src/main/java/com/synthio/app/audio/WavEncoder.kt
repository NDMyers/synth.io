package com.synthio.app.audio

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Simple WAV file encoder for high-quality audio exports.
 * Writes PCM float data as 16-bit WAV format.
 */
object WavEncoder {
    
    private const val SAMPLE_RATE = 48000
    private const val CHANNELS = 2 // Stereo
    private const val BITS_PER_SAMPLE = 16
    
    /**
     * Encode float PCM samples to WAV format.
     * 
     * @param samples Interleaved stereo float samples (L, R, L, R, ...)
     * @param outputStream Stream to write the WAV file
     * @return Number of bytes written
     */
    fun encode(samples: FloatArray, outputStream: OutputStream): Long {
        val numSamples = samples.size
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8
        val dataSize = numSamples * BITS_PER_SAMPLE / 8
        val fileSize = 36 + dataSize
        
        // Write WAV header
        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF chunk descriptor
            put("RIFF".toByteArray())
            putInt(fileSize)
            put("WAVE".toByteArray())
            
            // fmt sub-chunk
            put("fmt ".toByteArray())
            putInt(16) // Sub-chunk size for PCM
            putShort(1) // Audio format: 1 = PCM
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            
            // data sub-chunk
            put("data".toByteArray())
            putInt(dataSize)
        }
        
        outputStream.write(header.array())
        
        // Convert float samples to 16-bit PCM and write
        val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        var bytesWritten = 44L
        
        for (sample in samples) {
            // Clamp to [-1, 1] and convert to 16-bit signed integer
            val clamped = sample.coerceIn(-1f, 1f)
            val pcm16 = (clamped * 32767f).toInt().toShort()
            
            buffer.putShort(pcm16)
            
            if (!buffer.hasRemaining()) {
                outputStream.write(buffer.array())
                bytesWritten += buffer.capacity()
                buffer.clear()
            }
        }
        
        // Write remaining samples
        if (buffer.position() > 0) {
            outputStream.write(buffer.array(), 0, buffer.position())
            bytesWritten += buffer.position()
        }
        
        outputStream.flush()
        return bytesWritten
    }
    
    /**
     * Calculate the duration of audio data in milliseconds.
     */
    fun calculateDurationMs(numSamples: Int): Long {
        // numSamples is interleaved stereo, so divide by channels
        val frames = numSamples / CHANNELS
        return (frames * 1000L) / SAMPLE_RATE
    }
}
