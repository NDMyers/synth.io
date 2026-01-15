package com.synthio.app.audio

import kotlin.math.pow

/**
 * Music theory constants for the synthesizer.
 * Contains note frequencies, key signatures, and chord definitions.
 */
object MusicConstants {
    
    // A4 = 440 Hz (standard tuning)
    private const val A4_FREQUENCY = 440.0f
    private const val A4_MIDI_NOTE = 69
    
    /**
     * Chord types available for chord mode
     */
    enum class ChordType(val displayName: String) {
        TRIAD("Triads"),
        SEVENTH("7th Chords")
    }
    
    /**
     * Calculate frequency from MIDI note number.
     */
    fun midiNoteToFrequency(midiNote: Int): Float {
        return A4_FREQUENCY * 2f.pow((midiNote - A4_MIDI_NOTE) / 12f)
    }
    
    /**
     * All 12 key signatures with their scale intervals (semitones from root)
     * Major scale pattern: W-W-H-W-W-W-H (2-2-1-2-2-2-1)
     */
    enum class KeySignature(val displayName: String, val rootPitchClass: Int) {
        C("C", 0),
        C_SHARP("C♯/D♭", 1),
        D("D", 2),
        D_SHARP("D♯/E♭", 3),
        E("E", 4),
        F("F", 5),
        F_SHARP("F♯/G♭", 6),
        G("G", 7),
        G_SHARP("G♯/A♭", 8),
        A("A", 9),
        A_SHARP("A♯/B♭", 10),
        B("B", 11);
        
        /** Scale degrees for a major scale (0-based semitone offsets) */
        val scaleIntervals = listOf(0, 2, 4, 5, 7, 9, 11)
        
        /** 7th chord qualities for each scale degree: Maj7, m7, m7, Maj7, Dom7, m7, m7b5 */
        val seventhChordQualities = listOf("maj7", "m7", "m7", "maj7", "7", "m7", "m7♭5")
        
        /** Triad chord qualities for each scale degree: Major, minor, minor, Major, Major, minor, diminished */
        val triadChordQualities = listOf("", "m", "m", "", "", "m", "°")
        
        /** Get pitch classes (0-11) that are in this key's major scale */
        fun getScalePitchClasses(): List<Int> {
            return scaleIntervals.map { (rootPitchClass + it) % 12 }
        }
        
        /** Check if a pitch class (0-11) is diatonic to this key */
        fun isDiatonic(pitchClass: Int): Boolean {
            return getScalePitchClasses().contains(pitchClass)
        }
        
        /** Get the scale degree (1-7) for a diatonic pitch class, or null if not in key */
        fun getScaleDegree(pitchClass: Int): Int? {
            val scalePitches = getScalePitchClasses()
            val index = scalePitches.indexOf(pitchClass)
            return if (index >= 0) index + 1 else null
        }
    }
    
    /**
     * Single octave keyboard notes (C4 to B4)
     */
    enum class KeyboardNote(val midiNote: Int, val isBlackKey: Boolean, val displayName: String, val pitchClass: Int) {
        C4(60, false, "C", 0),
        CS4(61, true, "C♯", 1),
        D4(62, false, "D", 2),
        DS4(63, true, "D♯", 3),
        E4(64, false, "E", 4),
        F4(65, false, "F", 5),
        FS4(66, true, "F♯", 6),
        G4(67, false, "G", 7),
        GS4(68, true, "G♯", 8),
        A4(69, false, "A", 9),
        AS4(70, true, "A♯", 10),
        B4(71, false, "B", 11);
        
        /** Get display name appropriate for the key signature using flats if needed */
        fun getDisplayNameForKey(key: KeySignature): String {
            // Use flats for keys that are typically written with flats
            val usesFlats = key in listOf(
                KeySignature.F, KeySignature.D_SHARP, KeySignature.G_SHARP, 
                KeySignature.A_SHARP, KeySignature.C_SHARP
            )
            return when (pitchClass) {
                1 -> if (usesFlats) "D♭" else "C♯"
                3 -> if (usesFlats) "E♭" else "D♯"
                6 -> if (usesFlats) "G♭" else "F♯"
                8 -> if (usesFlats) "A♭" else "G♯"
                10 -> if (usesFlats) "B♭" else "A♯"
                else -> displayName
            }
        }
    }
    
    /**
     * Chord data class (works for both triads and 7th chords)
     */
    data class Chord(
        val name: String,
        val midiNotes: List<Int>
    )
    
    /** Note names for building chord names */
    private val noteNames = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val flatNoteNames = listOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")
    
    /**
     * Build a chord for a given scale degree in a key.
     * @param key The key signature
     * @param scaleDegree 1-7 (I through VII)
     * @param chordType TRIAD for 3-note chords, SEVENTH for 4-note chords
     * @param octaveBaseMidi The MIDI note for C in the target octave (e.g., 60 for C4)
     */
    fun buildChord(key: KeySignature, scaleDegree: Int, chordType: ChordType, octaveBaseMidi: Int = 60): Chord {
        val scalePitches = key.getScalePitchClasses()
        val degreeIndex = scaleDegree - 1
        
        // Build chord from scale degrees
        // Triad: root, 3rd, 5th (degrees 0, 2, 4)
        // Seventh: root, 3rd, 5th, 7th (degrees 0, 2, 4, 6)
        val chordDegreeOffsets = when (chordType) {
            ChordType.TRIAD -> listOf(0, 2, 4)
            ChordType.SEVENTH -> listOf(0, 2, 4, 6)
        }
        val chordDegrees = chordDegreeOffsets.map { (degreeIndex + it) % 7 }
        
        // Build midi notes ensuring they are in ascending order
        val midiNotes = mutableListOf<Int>()
        for (degree in chordDegrees) {
            val pitchClass = scalePitches[degree]
            var midiNote = octaveBaseMidi + pitchClass
            
            // Ensure this note is higher than the previous one
            if (midiNotes.isNotEmpty()) {
                while (midiNote <= midiNotes.last()) {
                    midiNote += 12
                }
            }
            midiNotes.add(midiNote)
        }
        
        // Build chord name
        val rootPitchClass = scalePitches[degreeIndex]
        val usesFlats = key in listOf(
            KeySignature.F, KeySignature.D_SHARP, KeySignature.G_SHARP,
            KeySignature.A_SHARP, KeySignature.C_SHARP
        )
        val rootName = if (usesFlats) flatNoteNames[rootPitchClass] else noteNames[rootPitchClass]
        val quality = when (chordType) {
            ChordType.TRIAD -> key.triadChordQualities[degreeIndex]
            ChordType.SEVENTH -> key.seventhChordQualities[degreeIndex]
        }
        val chordName = rootName + quality
        
        return Chord(chordName, midiNotes)
    }
    
    /**
     * Build a 7th chord for a given scale degree in a key (legacy function).
     */
    @Deprecated("Use buildChord with ChordType.SEVENTH", ReplaceWith("buildChord(key, scaleDegree, ChordType.SEVENTH, octaveBaseMidi)"))
    fun buildSeventhChord(key: KeySignature, scaleDegree: Int, octaveBaseMidi: Int = 60): Chord {
        return buildChord(key, scaleDegree, ChordType.SEVENTH, octaveBaseMidi)
    }
    
    /**
     * Get chord for a keyboard note in the given key.
     * Returns null if the note is not diatonic to the key.
     */
    fun getChordForNote(note: KeyboardNote, key: KeySignature, chordType: ChordType = ChordType.SEVENTH): Chord? {
        val scaleDegree = key.getScaleDegree(note.pitchClass) ?: return null
        return buildChord(key, scaleDegree, chordType, 60)
    }
    
    /**
     * Check if a keyboard note is playable in chord mode for the given key
     */
    fun isNotePlayableInKey(note: KeyboardNote, key: KeySignature): Boolean {
        return key.isDiatonic(note.pitchClass)
    }
    
    /**
     * Get the notes to play for a keyboard note.
     * In single mode: returns just the root note
     * In chord mode: returns the chord notes if diatonic, or single note if not
     */
    fun getNotesToPlay(
        note: KeyboardNote, 
        chordMode: Boolean, 
        key: KeySignature = KeySignature.C,
        chordType: ChordType = ChordType.SEVENTH
    ): List<Int> {
        if (!chordMode) {
            return listOf(note.midiNote)
        }
        
        val chord = getChordForNote(note, key, chordType)
        return chord?.midiNotes ?: listOf(note.midiNote)
    }
    
    /**
     * Get the chord name for display when playing a note in chord mode.
     */
    fun getChordName(note: KeyboardNote, key: KeySignature, chordType: ChordType): String? {
        val chord = getChordForNote(note, key, chordType)
        return chord?.name
    }
}
