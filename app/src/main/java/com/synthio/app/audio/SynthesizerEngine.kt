package com.synthio.app.audio

/**
 * Kotlin wrapper for the native audio engine.
 * Singleton pattern for global access.
 * 
 * Enhanced with Juno-106 style features:
 * - Global LFO with pitch/filter/PWM modulation
 * - Sub-oscillator and noise generator
 * - Stereo chorus effect
 * - Glide/Portamento
 * - Unison mode with detuning
 * - Key tracking for filter
 */
object SynthesizerEngine {
    
    init {
        System.loadLibrary("synthio")
    }
    
    private var isCreated = false
    private var isRunning = false
    
    fun create() {
        if (!isCreated) {
            nativeCreate()
            isCreated = true
        }
    }
    
    fun destroy() {
        if (isCreated) {
            stop()
            nativeDestroy()
            isCreated = false
        }
    }
    
    fun start(): Boolean {
        if (!isCreated) create()
        if (!isRunning) {
            isRunning = nativeStart()
        }
        return isRunning
    }
    
    fun stop() {
        if (isRunning) {
            nativeStop()
            isRunning = false
        }
    }
    
    fun noteOn(midiNote: Int, frequency: Float) {
        if (isRunning) {
            nativeNoteOn(midiNote, frequency)
        }
    }
    
    fun noteOff(midiNote: Int) {
        if (isRunning) {
            nativeNoteOff(midiNote)
        }
    }
    
    fun allNotesOff() {
        if (isRunning) {
            nativeAllNotesOff()
        }
    }
    
    // ===== OSCILLATOR PARAMETERS =====
    
    fun setWaveform(waveform: Waveform) {
        if (isCreated) {
            nativeSetWaveform(waveform.ordinal)
        }
    }
    
    fun toggleWaveform(waveform: Waveform, enabled: Boolean) {
        if (isCreated) {
            nativeToggleWaveform(waveform.ordinal, enabled)
        }
    }

    fun setPulseWidth(width: Float) {
        if (isCreated) {
            nativeSetPulseWidth(width)
        }
    }
    
    fun setSubOscLevel(level: Float) {
        if (isCreated) {
            nativeSetSubOscLevel(level)
        }
    }
    
    fun setNoiseLevel(level: Float) {
        if (isCreated) {
            nativeSetNoiseLevel(level)
        }
    }
    
    // ===== FILTER PARAMETERS =====
    
    fun setFilterCutoff(cutoffHz: Float) {
        if (isCreated) {
            nativeSetFilterCutoff(cutoffHz)
        }
    }
    
    fun setFilterResonance(resonance: Float) {
        if (isCreated) {
            nativeSetFilterResonance(resonance)
        }
    }
    
    fun setFilterEnvAmount(amount: Float) {
        if (isCreated) {
            nativeSetFilterEnvAmount(amount)
        }
    }
    
    fun setFilterKeyTracking(amount: Float) {
        if (isCreated) {
            nativeSetFilterKeyTracking(amount)
        }
    }
    
    fun setHPFCutoff(cutoffHz: Float) {
        if (isCreated) {
            nativeSetHPFCutoff(cutoffHz)
        }
    }
    
    // ===== ENVELOPE (ADSR) =====
    
    fun setAttack(time: Float) {
        if (isCreated) {
            nativeSetAttack(time)
        }
    }
    
    fun setDecay(time: Float) {
        if (isCreated) {
            nativeSetDecay(time)
        }
    }
    
    fun setSustain(level: Float) {
        if (isCreated) {
            nativeSetSustain(level)
        }
    }
    
    fun setRelease(time: Float) {
        if (isCreated) {
            nativeSetRelease(time)
        }
    }
    
    // ===== LFO PARAMETERS =====
    
    fun setLFORate(rateHz: Float) {
        if (isCreated) {
            nativeSetLFORate(rateHz)
        }
    }
    
    fun setLFOPitchDepth(depth: Float) {
        if (isCreated) {
            nativeSetLFOPitchDepth(depth)
        }
    }
    
    fun setLFOFilterDepth(depth: Float) {
        if (isCreated) {
            nativeSetLFOFilterDepth(depth)
        }
    }
    
    fun setLFOPWMDepth(depth: Float) {
        if (isCreated) {
            nativeSetLFOPWMDepth(depth)
        }
    }
    
    // ===== CHORUS =====
    
    fun setChorusMode(mode: ChorusMode) {
        if (isCreated) {
            nativeSetChorusMode(mode.ordinal)
        }
    }
    
    // ===== SYNTH EFFECTS (Delay, Reverb, Tremolo) =====
    
    fun setSynthTremolo(rate: Float, depth: Float) {
        if (isCreated) {
            nativeSetSynthTremoloRate(rate)
            nativeSetSynthTremoloDepth(depth)
        }
    }
    
    fun setSynthReverb(size: Float, mix: Float) {
        if (isCreated) {
            nativeSetSynthReverbSize(size)
            nativeSetSynthReverbMix(mix)
        }
    }
    
    fun setSynthDelay(time: Float, feedback: Float, mix: Float) {
        if (isCreated) {
            nativeSetSynthDelayTime(time)
            nativeSetSynthDelayFeedback(feedback)
            nativeSetSynthDelayMix(mix)
        }
    }
    
    // ===== GLIDE/PORTAMENTO =====
    
    fun setGlideTime(time: Float) {
        if (isCreated) {
            nativeSetGlideTime(time)
        }
    }
    
    fun setGlideEnabled(enabled: Boolean) {
        if (isCreated) {
            nativeSetGlideEnabled(enabled)
        }
    }
    
    // ===== UNISON MODE =====
    
    fun setUnisonEnabled(enabled: Boolean) {
        if (isCreated) {
            nativeSetUnisonEnabled(enabled)
        }
    }
    
    fun setUnisonVoices(count: Int) {
        if (isCreated) {
            nativeSetUnisonVoices(count)
        }
    }
    
    fun setUnisonDetune(cents: Float) {
        if (isCreated) {
            nativeSetUnisonDetune(cents)
        }
    }
    
    // ===== VOLUME CONTROLS =====
    
    fun setSynthVolume(volume: Float) {
        if (isCreated) {
            nativeSetSynthVolume(volume)
        }
    }
    
    fun setDrumVolume(volume: Float) {
        if (isCreated) {
            nativeSetDrumVolume(volume)
        }
    }
    
    fun setMetronomeVolume(volume: Float) {
        if (isCreated) {
            nativeSetMetronomeVolume(volume)
        }
    }
    
    // ===== DRUM MACHINE CONTROLS =====
    
    fun setDrumEnabled(enabled: Boolean) {
        if (isCreated) {
            nativeSetDrumEnabled(enabled)
        }
    }
    
    fun setDrumBPM(bpm: Float) {
        if (isCreated) {
            nativeSetDrumBPM(bpm)
        }
    }
    
    fun setKickEnabled(enabled: Boolean) {
        if (isCreated) {
            nativeSetKickEnabled(enabled)
        }
    }
    
    fun setSnareEnabled(enabled: Boolean) {
        if (isCreated) {
            nativeSetSnareEnabled(enabled)
        }
    }
    
    fun setHiHatEnabled(enabled: Boolean) {
        if (isCreated) {
            nativeSetHiHatEnabled(enabled)
        }
    }
    
    fun setHiHat16thNotes(is16th: Boolean) {
        if (isCreated) {
            nativeSetHiHat16thNotes(is16th)
        }
    }
    
    // ===== DRUM PATTERN CONTROLS =====
    
    fun setDrumStep(instrument: Int, step: Int, velocity: Float) {
        if (isCreated) {
            nativeSetDrumStep(instrument, step, velocity)
        }
    }
    
    fun getDrumStep(instrument: Int, step: Int): Float {
        return if (isCreated) nativeGetDrumStep(instrument, step) else 0f
    }
    
    fun toggleDrumStep(instrument: Int, step: Int) {
        if (isCreated) {
            nativeToggleDrumStep(instrument, step)
        }
    }
    
    fun setDrumInstrumentVolume(instrument: Int, volume: Float) {
        if (isCreated) {
            nativeSetDrumInstrumentVolume(instrument, volume)
        }
    }
    
    fun getDrumInstrumentVolume(instrument: Int): Float {
        return if (isCreated) nativeGetDrumInstrumentVolume(instrument) else 0f
    }
    
    fun resetDrumPattern() {
        if (isCreated) {
            nativeResetDrumPattern()
        }
    }
    
    // ===== WURLITZER CONTROLS =====
    
    fun setWurlitzerMode(enabled: Boolean) {
        if (isCreated) {
            nativeSetWurlitzerMode(enabled)
        }
    }
    
    fun setWurliTremolo(rate: Float, depth: Float) {
        if (isCreated) {
            nativeSetWurliTremolo(rate, depth)
        }
    }
    
    fun setWurliChorusMode(mode: ChorusMode) {
        if (isCreated) {
            nativeSetWurliChorusMode(mode.ordinal)
        }
    }
    
    fun setWurliReverb(size: Float, mix: Float) {
        if (isCreated) {
            nativeSetWurliReverb(size, mix)
        }
    }
    
    fun setWurliDelay(time: Float, feedback: Float, mix: Float) {
        if (isCreated) {
            nativeSetWurliDelay(time, feedback, mix)
        }
    }
    
    fun setWurliVolume(volume: Float) {
        if (isCreated) {
            nativeSetWurliVolume(volume)
        }
    }
    
    // ===== LOOPER CONTROLS =====
    
    fun looperStartRecording() {
        if (isCreated) {
            nativeLooperStartRecording()
        }
    }
    
    fun looperStartPlayback() {
        if (isCreated) {
            nativeLooperStartPlayback()
        }
    }
    
    fun looperStopPlayback() {
        if (isCreated) {
            nativeLooperStopPlayback()
        }
    }
    
    fun looperClearLoop() {
        if (isCreated) {
            nativeLooperClearLoop()
        }
    }
    
    fun getLooperState(): LooperState {
        if (isCreated) {
            return LooperState.fromInt(nativeGetLooperState())
        }
        return LooperState.IDLE
    }
    
    fun looperHasLoop(): Boolean {
        if (isCreated) {
            return nativeLooperHasLoop()
        }
        return false
    }
    
    fun getLooperCurrentBeat(): Int {
        if (isCreated) {
            return nativeGetLooperCurrentBeat()
        }
        return 0
    }
    
    fun getLooperCurrentBar(): Int {
        if (isCreated) {
            return nativeGetLooperCurrentBar()
        }
        return 0
    }
    
    // ===== MULTI-TRACK LOOPER =====
    
    fun looperStartRecordingTrack(trackIndex: Int) {
        if (isCreated) {
            nativeLooperStartRecordingTrack(trackIndex)
        }
    }
    
    fun looperClearTrack(trackIndex: Int) {
        if (isCreated) {
            nativeLooperClearTrack(trackIndex)
        }
    }
    
    fun looperClearAllTracks() {
        if (isCreated) {
            nativeLooperClearAllTracks()
        }
    }
    
    fun looperCancelRecording() {
        if (isCreated) {
            nativeLooperCancelRecording()
        }
    }
    
    fun looperSetTrackVolume(trackIndex: Int, volume: Float) {
        if (isCreated) {
            nativeLooperSetTrackVolume(trackIndex, volume)
        }
    }
    
    fun looperSetTrackMuted(trackIndex: Int, muted: Boolean) {
        if (isCreated) {
            nativeLooperSetTrackMuted(trackIndex, muted)
        }
    }
    
    fun looperSetTrackSolo(trackIndex: Int, solo: Boolean) {
        if (isCreated) {
            nativeLooperSetTrackSolo(trackIndex, solo)
        }
    }
    
    fun looperTrackHasContent(trackIndex: Int): Boolean {
        if (isCreated) {
            return nativeLooperTrackHasContent(trackIndex)
        }
        return false
    }
    
    fun looperGetTrackVolume(trackIndex: Int): Float {
        if (isCreated) {
            return nativeLooperGetTrackVolume(trackIndex)
        }
        return 0.7f
    }
    
    fun looperIsTrackMuted(trackIndex: Int): Boolean {
        if (isCreated) {
            return nativeLooperIsTrackMuted(trackIndex)
        }
        return false
    }
    
    fun looperIsTrackSolo(trackIndex: Int): Boolean {
        if (isCreated) {
            return nativeLooperIsTrackSolo(trackIndex)
        }
        return false
    }
    
    fun looperGetActiveRecordingTrack(): Int {
        if (isCreated) {
            return nativeLooperGetActiveRecordingTrack()
        }
        return -1
    }
    
    fun looperGetUsedTrackCount(): Int {
        if (isCreated) {
            return nativeLooperGetUsedTrackCount()
        }
        return 0
    }
    
    fun looperSetBarCount(bars: Int) {
        if (isCreated) {
            nativeLooperSetBarCount(bars)
        }
    }
    
    fun looperGetBarCount(): Int {
        if (isCreated) {
            return nativeLooperGetBarCount()
        }
        return 4 // Default
    }
    
    /**
     * Get mixed audio buffer for export.
     * @param trackMask Bitmask of tracks to include (bit 0 = track 0, etc.)
     * @return Interleaved stereo float samples, or null if no content
     */
    fun looperGetMixedBuffer(trackMask: Int): FloatArray? {
        if (isCreated) {
            return nativeLooperGetMixedBuffer(trackMask)
        }
        return null
    }
    
    /**
     * Get the size of the loop buffer (interleaved stereo samples)
     */
    fun looperGetBufferSize(): Long {
        if (isCreated) {
            return nativeLooperGetBufferSize()
        }
        return 0
    }
    
    // ===== NATIVE FUNCTION DECLARATIONS =====
    
    private external fun nativeCreate()
    private external fun nativeDestroy()
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeNoteOn(midiNote: Int, frequency: Float)
    private external fun nativeNoteOff(midiNote: Int)
    private external fun nativeAllNotesOff()
    
    // Oscillator
    private external fun nativeSetWaveform(waveform: Int)
    private external fun nativeToggleWaveform(waveform: Int, enabled: Boolean)
    private external fun nativeSetPulseWidth(width: Float)
    private external fun nativeSetSubOscLevel(level: Float)
    private external fun nativeSetNoiseLevel(level: Float)
    
    // Filter
    private external fun nativeSetFilterCutoff(cutoffHz: Float)
    private external fun nativeSetFilterResonance(resonance: Float)
    private external fun nativeSetFilterEnvAmount(amount: Float)
    private external fun nativeSetFilterKeyTracking(amount: Float)
    private external fun nativeSetHPFCutoff(cutoffHz: Float)
    
    // Envelope
    private external fun nativeSetAttack(time: Float)
    private external fun nativeSetDecay(time: Float)
    private external fun nativeSetSustain(level: Float)
    private external fun nativeSetRelease(time: Float)
    
    // LFO
    private external fun nativeSetLFORate(rateHz: Float)
    private external fun nativeSetLFOPitchDepth(depth: Float)
    private external fun nativeSetLFOFilterDepth(depth: Float)
    private external fun nativeSetLFOPWMDepth(depth: Float)
    
    // Chorus
    private external fun nativeSetChorusMode(mode: Int)
    
    // Synth Effects (Delay, Reverb, Tremolo)
    private external fun nativeSetSynthTremoloRate(rate: Float)
    private external fun nativeSetSynthTremoloDepth(depth: Float)
    private external fun nativeSetSynthReverbSize(size: Float)
    private external fun nativeSetSynthReverbMix(mix: Float)
    private external fun nativeSetSynthDelayTime(time: Float)
    private external fun nativeSetSynthDelayFeedback(feedback: Float)
    private external fun nativeSetSynthDelayMix(mix: Float)
    
    // Glide
    private external fun nativeSetGlideTime(time: Float)
    private external fun nativeSetGlideEnabled(enabled: Boolean)
    
    // Unison
    private external fun nativeSetUnisonEnabled(enabled: Boolean)
    private external fun nativeSetUnisonVoices(count: Int)
    private external fun nativeSetUnisonDetune(cents: Float)
    
    // Volume
    private external fun nativeSetSynthVolume(volume: Float)
    private external fun nativeSetDrumVolume(volume: Float)
    private external fun nativeSetMetronomeVolume(volume: Float)
    
    // Drum machine
    private external fun nativeSetDrumEnabled(enabled: Boolean)
    private external fun nativeSetDrumBPM(bpm: Float)
    private external fun nativeSetKickEnabled(enabled: Boolean)
    private external fun nativeSetSnareEnabled(enabled: Boolean)
    private external fun nativeSetHiHatEnabled(enabled: Boolean)
    private external fun nativeSetHiHat16thNotes(is16th: Boolean)
    
    // Drum pattern controls
    private external fun nativeSetDrumStep(instrument: Int, step: Int, velocity: Float)
    private external fun nativeGetDrumStep(instrument: Int, step: Int): Float
    private external fun nativeToggleDrumStep(instrument: Int, step: Int)
    private external fun nativeSetDrumInstrumentVolume(instrument: Int, volume: Float)
    private external fun nativeGetDrumInstrumentVolume(instrument: Int): Float
    private external fun nativeResetDrumPattern()
    
    // Wurlitzer
    private external fun nativeSetWurlitzerMode(enabled: Boolean)
    private external fun nativeSetWurliTremolo(rate: Float, depth: Float)
    private external fun nativeSetWurliChorusMode(mode: Int)
    private external fun nativeSetWurliReverb(size: Float, mix: Float)
    private external fun nativeSetWurliDelay(time: Float, feedback: Float, mix: Float)
    private external fun nativeSetWurliVolume(volume: Float)
    
    // Looper
    private external fun nativeLooperStartRecording()
    private external fun nativeLooperStartPlayback()
    private external fun nativeLooperStopPlayback()
    private external fun nativeLooperClearLoop()
    private external fun nativeGetLooperState(): Int
    private external fun nativeLooperHasLoop(): Boolean
    private external fun nativeGetLooperCurrentBeat(): Int
    private external fun nativeGetLooperCurrentBar(): Int
    
    // Multi-track looper
    private external fun nativeLooperStartRecordingTrack(trackIndex: Int)
    private external fun nativeLooperClearTrack(trackIndex: Int)
    private external fun nativeLooperClearAllTracks()
    private external fun nativeLooperCancelRecording()
    private external fun nativeLooperSetTrackVolume(trackIndex: Int, volume: Float)
    private external fun nativeLooperSetTrackMuted(trackIndex: Int, muted: Boolean)
    private external fun nativeLooperSetTrackSolo(trackIndex: Int, solo: Boolean)
    private external fun nativeLooperTrackHasContent(trackIndex: Int): Boolean
    private external fun nativeLooperGetTrackVolume(trackIndex: Int): Float
    private external fun nativeLooperIsTrackMuted(trackIndex: Int): Boolean
    private external fun nativeLooperIsTrackSolo(trackIndex: Int): Boolean
    private external fun nativeLooperGetActiveRecordingTrack(): Int
    private external fun nativeLooperGetUsedTrackCount(): Int
    private external fun nativeLooperSetBarCount(bars: Int)
    private external fun nativeLooperGetBarCount(): Int
    private external fun nativeLooperGetMixedBuffer(trackMask: Int): FloatArray?
    private external fun nativeLooperGetBufferSize(): Long
}

enum class Waveform {
    SINE,
    SQUARE,
    SAWTOOTH,
    TRIANGLE
}

enum class ChorusMode {
    OFF,
    MODE_I,
    MODE_II
}

enum class LooperState {
    IDLE,       // No loop, ready to record
    PRE_COUNT,  // Counting down before recording
    RECORDING,  // Recording user input
    STOPPED,    // Loop exists, not playing
    PLAYING;    // Loop is playing back
    
    companion object {
        fun fromInt(value: Int): LooperState {
            return entries.getOrElse(value) { IDLE }
        }
    }
}
