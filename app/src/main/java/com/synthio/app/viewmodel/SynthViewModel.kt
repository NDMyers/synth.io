package com.synthio.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthio.app.audio.AudioExportService
import com.synthio.app.audio.ChorusMode
import com.synthio.app.audio.LooperState
import com.synthio.app.audio.MidiHandler
import com.synthio.app.audio.MusicConstants
import com.synthio.app.audio.MusicConstants.ChordType
import com.synthio.app.audio.MusicConstants.KeySignature
import com.synthio.app.audio.MusicConstants.KeyboardNote
import com.synthio.app.audio.SynthesizerEngine
import com.synthio.app.audio.Waveform
import com.synthio.app.audio.ExportJob
import com.synthio.app.audio.ExportQuality
import com.synthio.app.audio.ExportStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    
    // ===== APPLICATION CONTEXT =====
    private var appContext: Context? = null
    private var audioExportService: AudioExportService? = null
    
    fun initContext(context: Context) {
        appContext = context.applicationContext
        audioExportService = AudioExportService(appContext!!)
    }
    
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
    // ===== OSCILLATOR PARAMETERS =====
    val activeWaveforms = mutableStateListOf(Waveform.SAWTOOTH)
    
    // Legacy support property (returns first active or default)
    val waveform: Waveform
        get() = activeWaveforms.firstOrNull() ?: Waveform.SAWTOOTH
    
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
    
    var metronomeVolume by mutableFloatStateOf(0.5f)
        private set
    
    // Looper bar count (1-8 bars)
    var looperBarCount by mutableIntStateOf(4)
        private set
    
    // Dialog for confirming bar count change when loops exist
    var showBarCountChangeDialog by mutableStateOf(false)
        private set
    var pendingBarCount by mutableIntStateOf(4)
        private set
    
    // ===== DRUM BEAT MAP =====
    var showDrumBeatMapModal by mutableStateOf(false)
    
    // Pattern state (16 steps per instrument, 0f = off, 1f = on)
    // Instrument indices: 0 = Kick, 1 = Snare, 2 = Hi-Hat
    var kickPattern = mutableStateListOf(*Array(16) { if (it == 0 || it == 8) 1f else 0f })
        private set
    var snarePattern = mutableStateListOf(*Array(16) { if (it == 4 || it == 12) 1f else 0f })
        private set
    var hiHatPattern = mutableStateListOf(*Array(16) { 
        when (it % 4) {
            0 -> 1.0f; 1 -> 0.5f; 2 -> 0.7f; else -> 0.4f 
        }
    })
        private set
    
    // Per-instrument volumes
    var kickVolume by mutableFloatStateOf(1f)
        private set
    var snareVolume by mutableFloatStateOf(1f)
        private set
    var hiHatVolume by mutableFloatStateOf(1f)
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
    
    // ===== AUDIO EXPORT STATE =====
    var showExportModal by mutableStateOf(false)
        private set
    
    var selectedExportTracks = mutableStateListOf<Int>()
        private set
    
    var includeExportDrums by mutableStateOf(false)
        private set
    
    var exportJobs = mutableStateListOf<ExportJob>()
        private set
    
    var showExportsPage by mutableStateOf(false)
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
    
    fun noteOn(note: KeyboardNote, octaveShift: Int = 0) {
        // In chord mode, only allow playable notes
        if (isChordMode && !MusicConstants.isNotePlayableInKey(note, selectedKeySignature)) {
            return
        }
        
        // Calculate octave offset from base octave 4 plus any shift
        val octaveOffset = (octave - 4 + octaveShift) * 12
        
        val notesToPlay = MusicConstants.getNotesToPlay(note, isChordMode, selectedKeySignature, selectedChordType)
            .map { it + octaveOffset } // Apply octave transposition
        
        // Store mapped notes with the octave shift so we can turn them off correctly
        // We need a unique key for the map if we want to support same note in different octaves
        // But activeNotes assumes just KeyboardNote key. 
        // For now, let's just append to the list if key exists, or create new.
        // ACTUALLY: If we play C4 and C5, both are KeyboardNote.C4. 
        // We need separate tracking. Map key should probably be Pair<KeyboardNote, Int> (note, shift).
        // For minimal refactor, let's just assume simple monophonic per key-octave for now,
        // or just accept that re-triggering same note name in different octave might overlap in tracking if we don't change the map key.
        // Changing map key is safer.
        
        // To avoid breaking change, let's handle the simple case first:
        // We need to track precisely which MIDI notes were turned on for this specific interaction.
        // But activeNotes is used for noteOff lookup.
        
        // Let's refactor activeNotes key to be String "NoteName+Shift" or similar, OR just handle it purely via MIDI?
        // Existing activeNotes is MutableMap<KeyboardNote, List<Int>>.
        // If I change it, I break usage elsewhere? No, it's private.
        
        // Wait, simpler approach: 
        // NoteOff needs to know which MIDI notes to turn off.
        // If I use (note, shift) as key... 
        
        // Let's try to not overengineer. If I press C4 (shift 0) -> plays 60. activeNotes[C4] = [60].
        // If I press C5 (shift 1) -> plays 72. activeNotes[C4] = [72]? No, that overwrites.
        // So I DO need a better key.
        
        // Let's change activeNotes key to a data class or Pair.
        // But `MusicConstants.KeyboardNote` is the key type everywhere.
        // I'll leave activeNotes as is for now but WARNING: playing C4 and C5 simultaneously might conflict in tracking.
        // For fullscreen keyboard, they are distinct touches.
        
        // Actually, I can allow multiple lists? 
        // activeNotes[note] can be List<Int> of ALL active midi notes for that key name?
        // noteOff(note, shift) would only remove the relevant ones?
        // That's tricky because we don't know which were triggered by which shift unless we store that.
        
        // Alternative: Just fire MIDI notes directly from FullscreenKeyboard?
        // But we want chord mode support.
        
        // Let's assume for now that FullscreenKeyboard won't be used with Chord Mode heavily across octaves?
        // Or better: Let's do the easy fix: `noteOn` creates text representation or specialized key.
        
        // Let's just update the map to use a composite key if possible.
        // Private val activeNotes = mutableMapOf<Any, List<Int>>() ?
        
        // OK, I will stick to the requested simple change: add `octaveShift` to signature.
        // And I'll update logic to simply ADD to the list for that key if it exists.
        // noteOff will try to remove specific MIDI notes corresponding to that shift.
        
        val newNotes = notesToPlay
        val existingNotes = activeNotes[note] ?: emptyList()
        activeNotes[note] = existingNotes + newNotes
        
        // Update chord display (only for base octave or just show chord name regardless)
        if (isChordMode) {
            currentChordName = MusicConstants.getChordName(note, selectedKeySignature, selectedChordType)
        } else {
            currentChordName = null
        }
        
        newNotes.forEach { midiNote ->
            val frequency = MusicConstants.midiNoteToFrequency(midiNote)
            SynthesizerEngine.noteOn(midiNote, frequency)
        }
    }
    
    fun noteOff(note: KeyboardNote, octaveShift: Int = 0) {
        // Calculate which midi notes correspond to this note+shift
        val octaveOffset = (octave - 4 + octaveShift) * 12
        // We can't regenerate randomness if chord had it, but standard chords are deterministic.
        // However, instead of recalculating, we should ideally look up what was played.
        
        // Since we appended to activeNotes[note], we now need to find and remove the subset.
        // Re-calculating expected MIDI notes:
        val expectedMidiNotes = MusicConstants.getNotesToPlay(note, isChordMode, selectedKeySignature, selectedChordType)
            .map { it + octaveOffset }
            
        val currentActive = activeNotes[note] ?: return
        
        // Find matches and turn them off
        expectedMidiNotes.forEach { midiNote ->
            if (currentActive.contains(midiNote)) {
                SynthesizerEngine.noteOff(midiNote)
            }
        }
        
        // Update map - remove the notes we just stopped
        val remaining = currentActive - expectedMidiNotes.toSet()
        if (remaining.isEmpty()) {
            activeNotes.remove(note)
            currentChordName = null
        } else {
            activeNotes[note] = remaining
        }
    }
    
    // ===== OSCILLATOR UPDATE FUNCTIONS =====
    
    fun toggleWaveform(waveform: Waveform) {
        if (activeWaveforms.contains(waveform)) {
            // Prevent removing the last waveform to ensure sound output
            if (activeWaveforms.size > 1) {
                activeWaveforms.remove(waveform)
                SynthesizerEngine.toggleWaveform(waveform, false)
            }
        } else {
            activeWaveforms.add(waveform)
            SynthesizerEngine.toggleWaveform(waveform, true)
        }
    }
    
    /**
     * Updates the waveform (Legavy: Exclusive selection)
     */
    fun updateWaveform(newWaveform: Waveform) {
        // Clear all others
        val toRemove = activeWaveforms.toList()
        toRemove.forEach { 
            if (it != newWaveform) {
                activeWaveforms.remove(it)
                SynthesizerEngine.toggleWaveform(it, false)
            }
        }
        
        // Ensure new one is active
        if (!activeWaveforms.contains(newWaveform)) {
            activeWaveforms.add(newWaveform)
            SynthesizerEngine.toggleWaveform(newWaveform, true)
        }
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
    
    // ===== DRUM BEAT MAP FUNCTIONS =====
    
    fun toggleDrumStep(instrument: Int, step: Int) {
        SynthesizerEngine.toggleDrumStep(instrument, step)
        // Sync local state
        when (instrument) {
            0 -> kickPattern[step] = if (kickPattern[step] > 0f) 0f else 1f
            1 -> snarePattern[step] = if (snarePattern[step] > 0f) 0f else 1f
            2 -> hiHatPattern[step] = if (hiHatPattern[step] > 0f) 0f else 1f
        }
    }
    
    fun updateDrumInstrumentVolume(instrument: Int, volume: Float) {
        SynthesizerEngine.setDrumInstrumentVolume(instrument, volume)
        when (instrument) {
            0 -> kickVolume = volume
            1 -> snareVolume = volume
            2 -> hiHatVolume = volume
        }
    }
    
    fun resetDrumPattern() {
        SynthesizerEngine.resetDrumPattern()
        // Sync local state
        for (i in 0 until 16) {
            kickPattern[i] = if (i == 0 || i == 8) 1f else 0f
            snarePattern[i] = if (i == 4 || i == 12) 1f else 0f
            hiHatPattern[i] = when (i % 4) {
                0 -> 1.0f; 1 -> 0.5f; 2 -> 0.7f; else -> 0.4f
            }
        }
        kickVolume = 1f
        snareVolume = 1f
        hiHatVolume = 1f
    }
    
    fun openDrumBeatMapModal() {
        showDrumBeatMapModal = true
    }
    
    fun closeDrumBeatMapModal() {
        showDrumBeatMapModal = false
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
        
        // Sync pattern: 8th notes mode clears odd steps, 16th note mode fills them
        for (i in 0 until 16) {
            if (i % 2 != 0) { // Odd steps are the 16th note subdivisions
                val isActive = hiHatPattern[i] > 0f
                if (isHiHat16thNotes) {
                    // Enable if not active (auto-fill 16ths)
                    if (!isActive) toggleDrumStep(2, i)
                } else {
                    // Disable if active (restrict to 8ths)
                    if (isActive) toggleDrumStep(2, i)
                }
            }
        }
    }
    
    fun updateDrumBPM(bpm: Float) {
        drumBPM = bpm
        SynthesizerEngine.setDrumBPM(bpm)
    }
    
    fun updateMetronomeVolume(volume: Float) {
        metronomeVolume = volume
        SynthesizerEngine.setMetronomeVolume(volume)
    }
    
    fun updateLooperBarCount(bars: Int) {
        val hasLoops = loopTracks.any { it.hasContent }
        if (hasLoops) {
            // Show confirmation dialog if there are existing loops
            pendingBarCount = bars
            showBarCountChangeDialog = true
        } else {
            // No loops, just change directly
            looperBarCount = bars.coerceIn(1, 8)
            SynthesizerEngine.looperSetBarCount(looperBarCount)
        }
    }
    
    fun confirmBarCountChange() {
        // Clear all tracks and apply new bar count
        loopTracks.forEachIndexed { index, _ ->
            SynthesizerEngine.looperClearTrack(index)
        }
        // Update loopTracks to show all are now empty
        loopTracks = List(4) { LoopTrackState() }
        looperBarCount = pendingBarCount.coerceIn(1, 8)
        SynthesizerEngine.looperSetBarCount(looperBarCount)
        showBarCountChangeDialog = false
    }
    
    fun dismissBarCountChangeDialog() {
        showBarCountChangeDialog = false
    }
    
    // ===== LOOPER CONTROLS =====
    
    fun loopButtonClicked() {
        // ALWAYS show looper modal to let user pick track
        // User requested removing the default "start recording track 0" logic
        showLooperModal = true
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
    
    fun cancelRecording() {
        SynthesizerEngine.looperCancelRecording()
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
        SynthesizerEngine.setMetronomeVolume(metronomeVolume)
        
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
    
    // ===== AUDIO EXPORT FUNCTIONS =====
    
    fun openExportModal() {
        // Pre-select tracks that have content
        selectedExportTracks.clear()
        loopTracks.forEachIndexed { index, track ->
            if (track.hasContent) {
                selectedExportTracks.add(index)
            }
        }
        includeExportDrums = isDrumEnabled
        showExportModal = true
    }
    
    fun closeExportModal() {
        showExportModal = false
    }
    
    fun toggleExportTrack(trackIndex: Int) {
        if (selectedExportTracks.contains(trackIndex)) {
            selectedExportTracks.remove(trackIndex)
        } else {
            selectedExportTracks.add(trackIndex)
        }
    }
    
    fun selectAllExportTracks() {
        selectedExportTracks.clear()
        loopTracks.forEachIndexed { index, track ->
            if (track.hasContent) {
                selectedExportTracks.add(index)
            }
        }
    }
    
    fun clearExportSelection() {
        selectedExportTracks.clear()
    }
    
    fun updateIncludeExportDrums(include: Boolean) {
        includeExportDrums = include
    }
    
    fun startExport(quality: ExportQuality) {
        // Build track mask from selection
        var trackMask = 0
        selectedExportTracks.forEach { index ->
            trackMask = trackMask or (1 shl index)
        }
        
        if (trackMask == 0) return // No tracks selected
        
        // Generate filename
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val extension = if (quality == ExportQuality.HIGH_QUALITY) "wav" else "m4a"
        val filename = "SynthIO_Export_$timestamp.$extension"
        
        // Create job
        val job = ExportJob(
            trackMask = trackMask,
            includeDrums = includeExportDrums,
            quality = quality,
            status = ExportStatus.PENDING,
            filename = filename
        )
        
        exportJobs.add(0, job)  // Add to front of list
        closeExportModal()
        
        // Launch background export
        performExport(job)
    }
    
    private fun performExport(job: ExportJob) {
        viewModelScope.launch(Dispatchers.Default) {
            val jobIndex = exportJobs.indexOfFirst { it.id == job.id }
            if (jobIndex < 0) return@launch
            
            // Update status to MIXING
            exportJobs[jobIndex] = exportJobs[jobIndex].copy(status = ExportStatus.MIXING)
            
            val exportService = audioExportService
            if (exportService == null) {
                exportJobs[jobIndex] = exportJobs[jobIndex].copy(status = ExportStatus.FAILED)
                return@launch
            }
            
            try {
                val result = exportService.exportAudio(
                    trackMask = job.trackMask,
                    includeDrums = job.includeDrums,
                    quality = if (job.quality == ExportQuality.HIGH_QUALITY) "high_quality" else "compressed"
                ) { progress ->
                    // Update progress
                    val idx = exportJobs.indexOfFirst { it.id == job.id }
                    if (idx >= 0) {
                        val status = when {
                            progress < 0.3f -> ExportStatus.MIXING
                            progress < 0.8f -> ExportStatus.ENCODING
                            else -> ExportStatus.ENCODING
                        }
                        exportJobs[idx] = exportJobs[idx].copy(
                            status = status,
                            progress = progress
                        )
                    }
                }
                
                val idx = exportJobs.indexOfFirst { it.id == job.id }
                if (idx >= 0) {
                    if (result != null) {
                        exportJobs[idx] = exportJobs[idx].copy(
                            status = ExportStatus.COMPLETE,
                            progress = 1f,
                            outputUri = Uri.parse(result.uri)
                        )
                    } else {
                        exportJobs[idx] = exportJobs[idx].copy(status = ExportStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                val idx = exportJobs.indexOfFirst { it.id == job.id }
                if (idx >= 0) {
                    exportJobs[idx] = exportJobs[idx].copy(status = ExportStatus.FAILED)
                }
            }
        }
    }
    
    fun cancelExport(jobId: String) {
        val jobIndex = exportJobs.indexOfFirst { it.id == jobId }
        if (jobIndex >= 0) {
            val job = exportJobs[jobIndex]
            if (job.status == ExportStatus.PENDING || job.status == ExportStatus.MIXING || job.status == ExportStatus.ENCODING) {
                exportJobs.removeAt(jobIndex)
            }
        }
    }
    
    fun removeExportJob(jobId: String) {
        val jobIndex = exportJobs.indexOfFirst { it.id == jobId }
        if (jobIndex >= 0) {
            exportJobs.removeAt(jobIndex)
        }
    }
    
    fun clearCompletedExports() {
        exportJobs.removeAll { it.status == ExportStatus.COMPLETE || it.status == ExportStatus.FAILED }
    }
    
    fun hasActiveExports(): Boolean {
        return exportJobs.any { 
            it.status == ExportStatus.PENDING || 
            it.status == ExportStatus.MIXING || 
            it.status == ExportStatus.ENCODING 
        }
    }
    
    fun openExportsPage() {
        showExportsPage = true
    }
    
    fun closeExportsPage() {
        showExportsPage = false
    }
    
    override fun onCleared() {
        super.onCleared()
        stopEngine()
    }
}
