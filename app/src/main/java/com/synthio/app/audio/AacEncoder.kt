package com.synthio.app.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AAC encoder using MediaCodec for compressed audio exports.
 * Produces raw AAC stream (ADTS format for playback compatibility).
 */
object AacEncoder {
    
    private const val TAG = "AacEncoder"
    private const val SAMPLE_RATE = 48000
    private const val CHANNELS = 2
    private const val BIT_RATE = 128000 // 128 kbps
    private const val TIMEOUT_US = 10000L
    
    /**
     * Encode float PCM samples to AAC format with ADTS headers.
     * 
     * @param samples Interleaved stereo float samples
     * @param outputStream Stream to write the AAC file
     * @return Number of bytes written
     */
    fun encode(samples: FloatArray, outputStream: OutputStream): Long {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            SAMPLE_RATE,
            CHANNELS
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        
        var bytesWritten = 0L
        var inputDone = false
        var sampleIndex = 0
        val bufferInfo = MediaCodec.BufferInfo()
        
        try {
            while (true) {
                // Feed input
                if (!inputDone) {
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                        inputBuffer.clear()
                        
                        // Convert float samples to 16-bit PCM for encoder
                        val samplesRemaining = samples.size - sampleIndex
                        val samplesToWrite = minOf(samplesRemaining, inputBuffer.capacity() / 2)
                        
                        if (samplesToWrite > 0) {
                            for (i in 0 until samplesToWrite) {
                                val sample = samples[sampleIndex + i].coerceIn(-1f, 1f)
                                val pcm16 = (sample * 32767f).toInt().toShort()
                                inputBuffer.putShort(pcm16)
                            }
                            sampleIndex += samplesToWrite
                            
                            val presentationTimeUs = (sampleIndex.toLong() * 1000000L) / (SAMPLE_RATE * CHANNELS)
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                samplesToWrite * 2,
                                presentationTimeUs,
                                0
                            )
                        } else {
                            // End of stream
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        }
                    }
                }
                
                // Drain output
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                        
                        if (bufferInfo.size > 0) {
                            // Write ADTS header
                            val adtsHeader = createAdtsHeader(bufferInfo.size + 7)
                            outputStream.write(adtsHeader)
                            bytesWritten += adtsHeader.size
                            
                            // Write AAC data
                            val data = ByteArray(bufferInfo.size)
                            outputBuffer.get(data)
                            outputStream.write(data)
                            bytesWritten += data.size
                        }
                        
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed: ${codec.outputFormat}")
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }
        
        outputStream.flush()
        return bytesWritten
    }
    
    /**
     * Create ADTS header for AAC frame.
     * Required for standalone AAC file playback.
     */
    private fun createAdtsHeader(frameLength: Int): ByteArray {
        val profile = 2 // AAC LC
        val freqIdx = 3 // 48000 Hz
        val chanCfg = 2 // Stereo
        
        return ByteArray(7).apply {
            // Syncword (0xFFF)
            this[0] = 0xFF.toByte()
            this[1] = 0xF9.toByte() // MPEG-4, no CRC
            
            // Profile, sample rate, private bit, channel config
            this[2] = ((profile - 1 shl 6) or (freqIdx shl 2) or (chanCfg shr 2)).toByte()
            this[3] = ((chanCfg and 3 shl 6) or (frameLength shr 11)).toByte()
            this[4] = ((frameLength shr 3) and 0xFF).toByte()
            this[5] = ((frameLength and 7 shl 5) or 0x1F).toByte()
            this[6] = 0xFC.toByte()
        }
    }
    
    /**
     * Calculate the duration of audio data in milliseconds.
     */
    fun calculateDurationMs(numSamples: Int): Long {
        val frames = numSamples / CHANNELS
        return (frames * 1000L) / SAMPLE_RATE
    }
}
