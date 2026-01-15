package com.synthio.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.synthio.app.audio.ChorusMode
import com.synthio.app.audio.LooperState
import com.synthio.app.audio.MidiHandler
import com.synthio.app.audio.MusicConstants
import com.synthio.app.audio.MusicConstants.ChordType
import com.synthio.app.audio.MusicConstants.KeySignature
import com.synthio.app.audio.MusicConstants.KeyboardNote
import com.synthio.app.audio.SynthesizerEngine
import com.synthio.app.audio.Waveform
import kotlinx.coroutines.flow.StateFlow

/**
 * State for a single loop track in the multi-track looper
 */
data class LoopTrackState(
    val hasContent: Boolean = false,
    val volume: Float = 0.7f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

class SynthViewModel : ViewModel() {
    
    // ===== MIDI =====
    private var midiHandler: MidiHandler? = null
    
    // Track MIDI notes separately from UI keyboard notes
    private val activeMidiNotes = mutableSetOf<Int>()
    
    // Sustain pedal state - notes held by sustain pedal
    private var isSustainPedalDown = false
    private val sustainedNotes = mutableSetOf<Int>()  // Notes being held by sustain
    
    // Mode state
    var isChordMode by mutableStateOf(false)
        private set
    
    // Key signature for chord mode
    var selectedKeySignature by mutableStateOf(KeySignature.C)
        private set
    
    // Chord type for chord mode (triads or 7th chords)
    var selectedChordType by mutableStateOf(ChordType.SEVENTH)
        private set
    
    // Dark mode
    var isDarkMode by mutableStateOf(false)
        private set
    
    // Current chord being played (for display)
    var currentChordName by mutableStateOf<String?>(null)
        private set
    
    // ===== OSCILLATOR PARAMETERS =====
    var waveform by mutableStateOf(Waveform.SAWTOOTH)
        private set
    
    var pulseWidth by mutableFloatStateOf(0.5f)
        private set
    
    var subOscLevel by mutableFloatStateOf(0.0f)
        private set
    
    var noiseLevel by mutableFloatStateOf(0.0f)
        private set
    
    // ===== FILTER PARAMETERS =====
    var filterCutoff by mutableFloatStateOf(10000f)
        private set
    
    var filterResonance by mutableFloatStateOf(0.0f)
        private set
    
    var filterKeyTracking by mutableFloatStateOf(0.0f)
        private set
    
    var hpfCutoff by mutableFloatStateOf(0.0f)
        private set
    
    // ===== ADSR PARAMETERS =====
    var attack by mutableFloatStateOf(0.01f)
        private set
    
    var decay by mutableFloatStateOf(0.2f)
        private set
    
    var sustain by mutableFloatStateOf(0.7f)
        private set
    
    var release by mutableFloatStateOf(0.3f)
        private set
    
    // ===== LFO PARAMETERS =====
    var lfoRate by mutableFloatStateOf(1.0f)
        private set
    
    var lfoPitchDepth by mutableFloatStateOf(0.0f)
        private set
    
    var lfoFilterDepth by mutableFloatStateOf(0.0f)
        private set
    
    var lfoPWMDepth by mutableFloatStateOf(0.0f)
        private set
    
    // ===== CHORUS =====
    var chorusMode by mutableStateOf(ChorusMode.OFF)
        private set
    
    // ===== GLIDE/PORTAMENTO =====
    var glideEnabled by mutableStateOf(false)
        private set
    
    var glideTime by mutableFloatStateOf(0.1f)
        private set
    
    // ===== UNISON MODE =====
    var unisonEnabled by mutableStateOf(false)
        private set
    
    var unisonVoices by mutableIntStateOf(4)
        private set
    
    var unisonDetune by mutableFloatStateOf(10.0f)
        private set
    
    // ===== SYNTH EFFECTS (Tremolo, Reverb, Delay) =====
    var synthTremoloRate by mutableFloatStateOf(5.0f)
        private set
    
    var synthTremoloDepth by mutableFloatStateOf(0.0f)
        private set
    
    var synthReverbSize by mutableFloatStateOf(0.3f)
        private set
    
    var synthReverbMix by mutableFloatStateOf(0.0f)
        private set
    
    var synthDelayTime by mutableFloatStateOf(0.25f)
        private set
    
    var synthDelayFeedback by mutableFloatStateOf(0.3f)
        private set
    
    var synthDelayMix by mutableFloatStateOf(0.0f)
        private set
        
    // ===== WURLITZER PARAMETERS =====
    var isWurlitzerMode by mutableStateOf(false)
        private set
        
    var wurliTremoloRate by mutableFloatStateOf(5.0f)
        private set
        
    var wurliTremoloDepth by mutableFloatStateOf(0.0f)
        private set
        
    var wurliChorusMode by mutableStateOf(ChorusMode.OFF)
        private set
        
    var wurliReverbSize by mutableFloatStateOf(0.3f)
        private set
        
    var wurliReverbMix by mutableFloatStateOf(0.0f)
        private set
        
    var wurliDelayTime by mutableFloatStateOf(0.25f)
        private set
        
    var wurliDelayFeedback by mutableFloatStateOf(0.3f)
        private set
        
    var wurliDelayMix by mutableFloatStateOf(0.0f)
        private set
        
    var wurliVolume by mutableFloatStateOf(0.7f)
        private set
    
    // ===== OCTAVE =====
    var octave by mutableIntStateOf(4) // Default to octave 4 (C4 = middle C)
        private set
    
    // ===== VOLUME =====
    var synthVolume by mutableFloatStateOf(0.7f)
        private set
    
    // ===== DRUM MACHINE =====
    var isDrumEnabled by mutableStateOf(false)
        private set
    
    var isKickEnabled by mutableStateOf(true)  // On by default
        private set
    
    var isSnareEnabled by mutableStateOf(true)  // On by default
        private set
    
    var isHiHatEnabled by mutableStateOf(false)
        private set
    
    var isHiHat16thNotes by mutableStateOf(true)  // true = 16th notes, false = 8th notes
        private set
    
    var drumBPM by mutableFloatStateOf(100f)
        private set
    
    var drumVolume by mutableFloatStateOf(0.7f)
        private set
    
    // ===== LOOPER =====
    var looperState by mutableStateOf(LooperState.IDLE)
        private set
    
    var looperHasLoop by mutableStateOf(false)
        private set
    
    var looperCurrentBeat by mutableIntStateOf(0)
        private set
    
    var looperCurrentBar by mutableIntStateOf(0)
        private set
    
    var showLoopOverrideDialog by mutableStateOf(false)
        private set
    
    // Multi-track looper state
    var loopTracks by mutableStateOf(List(4) { LoopTrackState() })
        private set
    
    var showLooperModal by mutableStateOf(false)
        private set
    
    var deleteTrackConfirmIndex by mutableStateOf<Int?>(null)
        private set
    
    var showDeleteAllDialog by mutableStateOf(false)
        private set
    
    var activeRecordingTrack by mutableIntStateOf(-1)
        private set
    
    // ===== MIDI STATE =====
    var isMidiAvailable by mutableStateOf(false)
        private set
    
    var isMidiDeviceConnected by mutableStateOf(false)
        private set
    
    // Track currently playing notes to handle chord mode note offs
    private val activeNotes = mutableMapOf<KeyboardNote, List<Int>>()
    
    fun startEngine() {
        SynthesizerEngine.create()
        SynthesizerEngine.start()
        applyAllParameters()
    }
    
    fun stopEngine() {
        SynthesizerEngine.allNotesOff()
        SynthesizerEngine.stop()
        SynthesizerEngine.destroy()
        releaseMidi()
    }
    
    // ===== MIDI FUNCTIONS =====
    
    /**
     * Initialize MIDI support. Call this from the Activity with a Context.
     */
    fun initializeMidi(context: Context) {
        if (midiHandler != null) return  // Already initialized
        
        midiHandler = MidiHandler(context).apply {
            // Set up callbacks for MIDI events
            onNoteOn = { midiNote, velocity ->
                midiNoteOn(midiNote, velocity)
            }
            onNoteOff = { midiNote ->
                midiNoteOff(midiNote)
            }
            onControlChange = { controller, value ->
                handleMidiControlChange(controller, value)
            }
            onSustainPedal = { isPressed ->
                handleSustainPedal(isPressed)
            }
        }
        
        val initialized = midiHandler?.initialize() ?: false
        isMidiAvailable = initialized
        
        // Observe MIDI device connection state
        // Note: This is a simplified approach - in a real app you'd use proper coroutine collection
    }
    
    /**
     * Handle MIDI Note On - plays the exact MIDI note (full 128-note range)
     */
    private fun midiNoteOn(midiNote: Int, velocity: Int) {
        if (activeMidiNotes.contains(midiNote)) return  // Avoid duplicate note-ons
        
        activeMidiNotes.add(midiNote)
        
        // Convert velocity (0-127) to normalized float (0.0-1.0)
        val normalizedVelocity = velocity / 127f
        
        // Calculate frequency for this MIDI note
        val frequency = MusicConstants.midiNoteToFrequency(midiNote)
        
        // Play the note directly - no chord mode, no octave offset for MIDI input
        SynthesizerEngine.noteOn(midiNote, frequency)
        
        // Update UI state to show MIDI activity
        isMidiDeviceConnected = true
    }
    
    /**
     * Handle MIDI Note Off
     * If sustain pedal is down, the note is held until the pedal is released
     */
    private fun midiNoteOff(midiNote: Int) {
        if (!activeMidiNotes.contains(midiNote)) return
        
        activeMidiNotes.remove(midiNote)
        
        // If sustain pedal is down, add to sustained notes instead of releasing
        if (isSustainPedalDown) {
            sustainedNotes.add(midiNote)
            // Don't release the note - it will be released when pedal goes up
        } else {
            SynthesizerEngine.noteOff(midiNote)
        }
    }
    
    /**
     * Handle sustain pedal (CC64)
     * When pedal is pressed: hold all notes even after key release
     * When pedal is released: release all sustained notes
     */
    private fun handleSustainPedal(isPressed: Boolean) {
        isSustainPedalDown = isPressed
        
        if (!isPressed) {
            // Pedal released - release all sustained notes that aren't currently being played
            for (midiNote in sustainedNotes) {
                // Only release if the key isn't still being held down
                if (!activeMidiNotes.contains(midiNote)) {
                    SynthesizerEngine.noteOff(midiNote)
                }
            }
            sustainedNotes.clear()
        }
    }
    
    /**
     * Handle MIDI Control Change messages
     * Common controllers:
     * - 1: Modulation wheel -> LFO pitch depth (vibrato)
     * - 7: Volume
     * - 64: Sustain pedal (handled separately via onSustainPedal callback)
     * - 74: Filter cutoff
     */
    private fun handleMidiControlChange(controller: Int, value: Int) {
        when (controller) {
            MidiHandler.CC_MODULATION -> {
                // Modulation wheel - map to vibrato (LFO pitch depth)
                val normalized = value / 127f * 0.5f  // Max 50% LFO depth
                updateLFOPitchDepth(normalized)
            }
            MidiHandler.CC_VOLUME -> {
                // Volume
                val normalized = value / 127f
                updateSynthVolume(normalized)
            }
            MidiHandler.CC_FILTER_CUTOFF -> {
                // Filter cutoff (common mapping)
                val cutoff = 100f + (value / 127f) * 14900f  // 100Hz to 15000Hz
                updateFilterCutoff(cutoff)
            }
            // Note: CC_SUSTAIN_PEDAL (64) is handled via onSustainPedal callback
        }
    }
    
    /**
     * Update MIDI connection state (call periodically or from StateFlow collector)
     */
    fun updateMidiState() {
        isMidiDeviceConnected = midiHandler?.isDeviceConnected?.value ?: false
    }
    
    /**
     * Get the MIDI device connection StateFlow for UI observation
     */
    fun getMidiConnectionState(): StateFlow<Boolean>? = midiHandler?.isDeviceConnected
    
    /**
     * Rescan for MIDI devices
     */
    fun rescanMidiDevices() {
        midiHandler?.scanForDevices()
    }
    
    /**
     * Release MIDI resources
     */
    private fun releaseMidi() {
        midiHandler?.release()
        midiHandler = null
        activeMidiNotes.clear()
        sustainedNotes.clear()
        isSustainPedalDown = false
    }
    
    fun toggleChordMode() {
        // Release all current notes first
        SynthesizerEngine.allNotesOff()
        activeNotes.clear()
        currentChordName = null
        
        isChordMode = !isChordMode
    }
    
    fun updateKeySignature(key: KeySignature) {
        // Release all current notes first when changing key
        SynthesizerEngine.allNotesOff()
        activeNotes.clear()
        currentChordName = null
        
        selectedKeySignature = key
    }
    
    fun updateChordType(type: ChordType) {
        // Release all current notes first when changing chord type
        SynthesizerEngine.allNotesOff()
        activeNotes.clear()
        currentChordName = null
        
        selectedChordType = type
    }
    
    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }
    
    fun noteOn(note: KeyboardNote) {
        // In chord mode, only allow playable notes
        if (isChordMode && !MusicConstants.isNotePlayableInKey(note, selectedKeySignature)) {
            return
        }
        
        // Calculate octave offset from base octave 4
        val octaveOffset = (octave - 4) * 12
        
        val notesToPlay = MusicConstants.getNotesToPlay(note, isChordMode, selectedKeySignature, selectedChordType)
            .map { it + octaveOffset } // Apply octave transposition
        activeNotes[note] = notesToPlay
        
        // Update chord display
        if (isChordMode) {
            currentChordName = MusicConstants.getChordName(note, selectedKeySignature, selectedChordType)
        } else {
            currentChordName = null
        }
        
        notesToPlay.forEach { midiNote ->
            val frequency = MusicConstants.midiNoteToFrequency(midiNote)
            SynthesizerEngine.noteOn(midiNote, frequency)
        }
    }
    
    fun noteOff(note: KeyboardNote) {
        activeNotes[note]?.forEach { midiNote ->
            SynthesizerEngine.noteOff(midiNote)
        }
        activeNotes.remove(note)
        
        if (activeNotes.isEmpty()) {
            currentChordName = null
        }
    }
    
    // ===== OSCILLATOR UPDATE FUNCTIONS =====
    
    fun updateWaveform(newWaveform: Waveform) {
        waveform = newWaveform
        SynthesizerEngine.setWaveform(newWaveform)
    }
    
    fun updatePulseWidth(width: Float) {
        pulseWidth = width
        SynthesizerEngine.setPulseWidth(width)
    }
    
    fun updateSubOscLevel(level: Float) {
        subOscLevel = level
        SynthesizerEngine.setSubOscLevel(level)
    }
    
    fun updateNoiseLevel(level: Float) {
        noiseLevel = level
        SynthesizerEngine.setNoiseLevel(level)
    }
    
    // ===== FILTER UPDATE FUNCTIONS =====
    
    fun updateFilterCutoff(cutoff: Float) {
        filterCutoff = cutoff
        SynthesizerEngine.setFilterCutoff(cutoff)
    }
    
    fun updateFilterResonance(resonance: Float) {
        filterResonance = resonance
        SynthesizerEngine.setFilterResonance(resonance)
    }
    
    fun updateFilterKeyTracking(amount: Float) {
        filterKeyTracking = amount
        SynthesizerEngine.setFilterKeyTracking(amount)
    }
    
    fun updateHPFCutoff(cutoff: Float) {
        hpfCutoff = cutoff
        SynthesizerEngine.setHPFCutoff(cutoff)
    }
    
    // ===== ENVELOPE UPDATE FUNCTIONS =====
    
    fun updateAttack(time: Float) {
        attack = time
        SynthesizerEngine.setAttack(time)
    }
    
    fun updateDecay(time: Float) {
        decay = time
        SynthesizerEngine.setDecay(time)
    }
    
    fun updateSustain(level: Float) {
        sustain = level
        SynthesizerEngine.setSustain(level)
    }
    
    fun updateRelease(time: Float) {
        release = time
        SynthesizerEngine.setRelease(time)
    }
    
    // ===== LFO UPDATE FUNCTIONS =====
    
    fun updateLFORate(rate: Float) {
        lfoRate = rate
        SynthesizerEngine.setLFORate(rate)
    }
    
    fun updateLFOPitchDepth(depth: Float) {
        lfoPitchDepth = depth
        SynthesizerEngine.setLFOPitchDepth(depth)
    }
    
    fun updateLFOFilterDepth(depth: Float) {
        lfoFilterDepth = depth
        SynthesizerEngine.setLFOFilterDepth(depth)
    }
    
    fun updateLFOPWMDepth(depth: Float) {
        lfoPWMDepth = depth
        SynthesizerEngine.setLFOPWMDepth(depth)
    }
    
    // ===== CHORUS UPDATE FUNCTIONS =====
    
    fun updateChorusMode(mode: ChorusMode) {
        chorusMode = mode
        SynthesizerEngine.setChorusMode(mode)
    }
    
    // ===== GLIDE UPDATE FUNCTIONS =====
    
    fun toggleGlide() {
        glideEnabled = !glideEnabled
        SynthesizerEngine.setGlideEnabled(glideEnabled)
    }
    
    fun updateGlideTime(time: Float) {
        glideTime = time
        SynthesizerEngine.setGlideTime(time)
    }
    
    // ===== UNISON UPDATE FUNCTIONS =====
    
    fun toggleUnison() {
        unisonEnabled = !unisonEnabled
        SynthesizerEngine.setUnisonEnabled(unisonEnabled)
    }
    
    fun updateUnisonVoices(count: Int) {
        unisonVoices = count
        SynthesizerEngine.setUnisonVoices(count)
    }
    
    fun updateUnisonDetune(cents: Float) {
        unisonDetune = cents
        SynthesizerEngine.setUnisonDetune(cents)
    }
    
    // ===== SYNTH EFFECTS UPDATE FUNCTIONS =====
    
    fun updateSynthTremolo(rate: Float, depth: Float) {
        synthTremoloRate = rate
        synthTremoloDepth = depth
        SynthesizerEngine.setSynthTremolo(rate, depth)
    }
    
    fun updateSynthReverb(size: Float, mix: Float) {
        synthReverbSize = size
        synthReverbMix = mix
        SynthesizerEngine.setSynthReverb(size, mix)
    }
    
    fun updateSynthDelay(time: Float, feedback: Float, mix: Float) {
        synthDelayTime = time
        synthDelayFeedback = feedback
        synthDelayMix = mix
        SynthesizerEngine.setSynthDelay(time, feedback, mix)
    }
    
    // ===== VOLUME UPDATE FUNCTIONS =====
    
    fun updateSynthVolume(volume: Float) {
        synthVolume = volume
        SynthesizerEngine.setSynthVolume(volume)
    }
    
    // ===== OCTAVE UPDATE FUNCTIONS =====
    
    fun updateOctave(newOctave: Int) {
        // Clamp octave to valid range (1-7)
        octave = newOctave.coerceIn(1, 7)
    }
    
    fun octaveUp() {
        if (octave < 7) octave++
    }
    
    fun octaveDown() {
        if (octave > 1) octave--
    }
    
    fun updateDrumVolume(volume: Float) {
        drumVolume = volume
        SynthesizerEngine.setDrumVolume(volume)
    }
    
    // ===== DRUM MACHINE FUNCTIONS =====
    
    fun toggleDrumMachine() {
        isDrumEnabled = !isDrumEnabled
        SynthesizerEngine.setDrumEnabled(isDrumEnabled)
    }
    
    fun toggleHiHat() {
        isHiHatEnabled = !isHiHatEnabled
        SynthesizerEngine.setHiHatEnabled(isHiHatEnabled)
    }
    
    fun toggleKick() {
        isKickEnabled = !isKickEnabled
        SynthesizerEngine.setKickEnabled(isKickEnabled)
    }
    
    fun toggleSnare() {
        isSnareEnabled = !isSnareEnabled
        SynthesizerEngine.setSnareEnabled(isSnareEnabled)
    }
    
    fun toggleHiHatMode() {
        isHiHat16thNotes = !isHiHat16thNotes
        SynthesizerEngine.setHiHat16thNotes(isHiHat16thNotes)
    }
    
    fun updateDrumBPM(bpm: Float) {
        drumBPM = bpm
        SynthesizerEngine.setDrumBPM(bpm)
    }
    
    // ===== LOOPER CONTROLS =====
    
    fun loopButtonClicked() {
        if (looperHasLoop) {
            // Loop exists - show the looper modal for multi-track management
            showLooperModal = true
        } else {
            // No loop yet - start recording track 0 immediately
            startRecordingTrack(0)
        }
    }
    
    fun dismissLoopOverrideDialog() {
        showLoopOverrideDialog = false
    }
    
    fun confirmLoopOverride() {
        showLoopOverrideDialog = false
        SynthesizerEngine.looperClearLoop()
        startRecordingTrack(0)
    }
    
    private fun startLoopRecording() {
        SynthesizerEngine.looperStartRecording()
        updateLooperState()
    }
    
    fun playStopLoop() {
        when (looperState) {
            LooperState.PLAYING -> {
                SynthesizerEngine.looperStopPlayback()
            }
            LooperState.STOPPED -> {
                SynthesizerEngine.looperStartPlayback()
            }
            else -> { /* ignore */ }
        }
        updateLooperState()
    }
    
    fun updateLooperState() {
        looperState = SynthesizerEngine.getLooperState()
        looperHasLoop = SynthesizerEngine.looperHasLoop()
        looperCurrentBeat = SynthesizerEngine.getLooperCurrentBeat()
        looperCurrentBar = SynthesizerEngine.getLooperCurrentBar()
        activeRecordingTrack = SynthesizerEngine.looperGetActiveRecordingTrack()
        refreshLoopTrackStates()
    }
    
    fun clearLoop() {
        SynthesizerEngine.looperClearLoop()
        updateLooperState()
    }
    
    // ===== MULTI-TRACK LOOPER =====
    
    fun showLooperModalDialog() {
        showLooperModal = true
    }
    
    fun dismissLooperModal() {
        showLooperModal = false
    }
    
    fun startRecordingTrack(trackIndex: Int) {
        SynthesizerEngine.looperStartRecordingTrack(trackIndex)
        showLooperModal = false  // Close modal when recording starts
        updateLooperState()
    }
    
    fun updateTrackVolume(trackIndex: Int, volume: Float) {
        SynthesizerEngine.looperSetTrackVolume(trackIndex, volume)
        refreshLoopTrackStates()
    }
    
    fun toggleTrackMute(trackIndex: Int) {
        val currentMuted = loopTracks.getOrNull(trackIndex)?.isMuted ?: false
        SynthesizerEngine.looperSetTrackMuted(trackIndex, !currentMuted)
        refreshLoopTrackStates()
    }
    
    fun toggleTrackSolo(trackIndex: Int) {
        val currentSolo = loopTracks.getOrNull(trackIndex)?.isSolo ?: false
        SynthesizerEngine.looperSetTrackSolo(trackIndex, !currentSolo)
        refreshLoopTrackStates()
    }
    
    fun showDeleteTrackConfirmation(trackIndex: Int) {
        deleteTrackConfirmIndex = trackIndex
    }
    
    fun dismissDeleteTrackConfirmation() {
        deleteTrackConfirmIndex = null
    }
    
    fun confirmDeleteTrack() {
        deleteTrackConfirmIndex?.let { index ->
            SynthesizerEngine.looperClearTrack(index)
            updateLooperState()
        }
        deleteTrackConfirmIndex = null
    }
    
    fun showDeleteAllConfirmation() {
        showDeleteAllDialog = true
    }
    
    fun dismissDeleteAllConfirmation() {
        showDeleteAllDialog = false
    }
    
    fun confirmDeleteAllTracks() {
        SynthesizerEngine.looperClearAllTracks()
        showDeleteAllDialog = false
        showLooperModal = false  // Close modal after deleting all
        updateLooperState()
    }
    
    fun refreshLoopTrackStates() {
        loopTracks = List(4) { index ->
            LoopTrackState(
                hasContent = SynthesizerEngine.looperTrackHasContent(index),
                volume = SynthesizerEngine.looperGetTrackVolume(index),
                isMuted = SynthesizerEngine.looperIsTrackMuted(index),
                isSolo = SynthesizerEngine.looperIsTrackSolo(index)
            )
        }
    }
    
    // ===== WURLITZER CONTROLS =====
    fun updateWurlitzerMode(enabled: Boolean) {
        isWurlitzerMode = enabled
        SynthesizerEngine.setWurlitzerMode(enabled)
    }
    
    fun updateWurliTremolo(rate: Float, depth: Float) {
        wurliTremoloRate = rate
        wurliTremoloDepth = depth
        SynthesizerEngine.setWurliTremolo(rate, depth)
    }
    
    fun updateWurliChorusMode(mode: ChorusMode) {
        wurliChorusMode = mode
        SynthesizerEngine.setWurliChorusMode(mode)
    }
    
    fun updateWurliReverb(size: Float, mix: Float) {
        wurliReverbSize = size
        wurliReverbMix = mix
        SynthesizerEngine.setWurliReverb(size, mix)
    }
    
    fun updateWurliDelay(time: Float, feedback: Float, mix: Float) {
        wurliDelayTime = time
        wurliDelayFeedback = feedback
        wurliDelayMix = mix
        SynthesizerEngine.setWurliDelay(time, feedback, mix)
    }
    
    fun updateWurliVolume(volume: Float) {
        wurliVolume = volume
        SynthesizerEngine.setWurliVolume(volume)
    }
    
    private fun applyAllParameters() {
        // Oscillator
        SynthesizerEngine.setWaveform(waveform)
        SynthesizerEngine.setPulseWidth(pulseWidth)
        SynthesizerEngine.setSubOscLevel(subOscLevel)
        SynthesizerEngine.setNoiseLevel(noiseLevel)
        
        // Filter
        SynthesizerEngine.setFilterCutoff(filterCutoff)
        SynthesizerEngine.setFilterResonance(filterResonance)
        SynthesizerEngine.setFilterKeyTracking(filterKeyTracking)
        SynthesizerEngine.setHPFCutoff(hpfCutoff)
        
        // Envelope
        SynthesizerEngine.setAttack(attack)
        SynthesizerEngine.setDecay(decay)
        SynthesizerEngine.setSustain(sustain)
        SynthesizerEngine.setRelease(release)
        
        // LFO
        SynthesizerEngine.setLFORate(lfoRate)
        SynthesizerEngine.setLFOPitchDepth(lfoPitchDepth)
        SynthesizerEngine.setLFOFilterDepth(lfoFilterDepth)
        SynthesizerEngine.setLFOPWMDepth(lfoPWMDepth)
        
        // Chorus
        SynthesizerEngine.setChorusMode(chorusMode)
        
        // Glide
        SynthesizerEngine.setGlideEnabled(glideEnabled)
        SynthesizerEngine.setGlideTime(glideTime)
        
        // Unison
        SynthesizerEngine.setUnisonEnabled(unisonEnabled)
        SynthesizerEngine.setUnisonVoices(unisonVoices)
        SynthesizerEngine.setUnisonDetune(unisonDetune)
        
        // Synth Effects
        SynthesizerEngine.setSynthTremolo(synthTremoloRate, synthTremoloDepth)
        SynthesizerEngine.setSynthReverb(synthReverbSize, synthReverbMix)
        SynthesizerEngine.setSynthDelay(synthDelayTime, synthDelayFeedback, synthDelayMix)
        
        // Volume
        SynthesizerEngine.setSynthVolume(synthVolume)
        SynthesizerEngine.setDrumVolume(drumVolume)
        
        // Drum machine
        SynthesizerEngine.setDrumBPM(drumBPM)
        SynthesizerEngine.setDrumEnabled(isDrumEnabled)
        SynthesizerEngine.setKickEnabled(isKickEnabled)
        SynthesizerEngine.setSnareEnabled(isSnareEnabled)
        SynthesizerEngine.setHiHatEnabled(isHiHatEnabled)
        SynthesizerEngine.setHiHat16thNotes(isHiHat16thNotes)
        
        // Wurlitzer
        SynthesizerEngine.setWurlitzerMode(isWurlitzerMode)
        SynthesizerEngine.setWurliTremolo(wurliTremoloRate, wurliTremoloDepth)
        SynthesizerEngine.setWurliChorusMode(wurliChorusMode)
        SynthesizerEngine.setWurliReverb(wurliReverbSize, wurliReverbMix)
        SynthesizerEngine.setWurliDelay(wurliDelayTime, wurliDelayFeedback, wurliDelayMix)
        SynthesizerEngine.setWurliVolume(wurliVolume)
    }
    
    override fun onCleared() {
        super.onCleared()
        stopEngine()
    }
}
