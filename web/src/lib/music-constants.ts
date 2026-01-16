/**
 * Music theory constants for Synth.io Web
 * Ported from Android MusicConstants.kt
 */

// Waveform and Chord types
export type ChordType = 'triad' | 'seventh';

// All 12 key signatures with their scale intervals
export const KEY_SIGNATURES = [
    { id: 'C', displayName: 'C', rootPitchClass: 0 },
    { id: 'C_SHARP', displayName: 'C♯/D♭', rootPitchClass: 1 },
    { id: 'D', displayName: 'D', rootPitchClass: 2 },
    { id: 'D_SHARP', displayName: 'D♯/E♭', rootPitchClass: 3 },
    { id: 'E', displayName: 'E', rootPitchClass: 4 },
    { id: 'F', displayName: 'F', rootPitchClass: 5 },
    { id: 'F_SHARP', displayName: 'F♯/G♭', rootPitchClass: 6 },
    { id: 'G', displayName: 'G', rootPitchClass: 7 },
    { id: 'G_SHARP', displayName: 'G♯/A♭', rootPitchClass: 8 },
    { id: 'A', displayName: 'A', rootPitchClass: 9 },
    { id: 'A_SHARP', displayName: 'A♯/B♭', rootPitchClass: 10 },
    { id: 'B', displayName: 'B', rootPitchClass: 11 },
] as const;

export type KeySignatureId = typeof KEY_SIGNATURES[number]['id'];

export interface KeySignature {
    id: KeySignatureId;
    displayName: string;
    rootPitchClass: number;
}

// Major scale pattern: W-W-H-W-W-W-H (2-2-1-2-2-2-1)
const SCALE_INTERVALS = [0, 2, 4, 5, 7, 9, 11];

// 7th chord qualities: Maj7, m7, m7, Maj7, Dom7, m7, m7♭5
const SEVENTH_CHORD_QUALITIES = ['maj7', 'm7', 'm7', 'maj7', '7', 'm7', 'm7♭5'];

// Triad qualities: Major, minor, minor, Major, Major, minor, diminished
const TRIAD_CHORD_QUALITIES = ['', 'm', 'm', '', '', 'm', '°'];

// Note names
const NOTE_NAMES = ['C', 'C♯', 'D', 'D♯', 'E', 'F', 'F♯', 'G', 'G♯', 'A', 'A♯', 'B'];
const FLAT_NOTE_NAMES = ['C', 'D♭', 'D', 'E♭', 'E', 'F', 'G♭', 'G', 'A♭', 'A', 'B♭', 'B'];

// Keys that typically use flats
const FLAT_KEYS: KeySignatureId[] = ['F', 'D_SHARP', 'G_SHARP', 'A_SHARP', 'C_SHARP'];

/**
 * Get the key signature object by ID
 */
export function getKeySignature(id: KeySignatureId): KeySignature {
    return KEY_SIGNATURES.find(k => k.id === id) || KEY_SIGNATURES[0];
}

/**
 * Get pitch classes (0-11) in a major scale for the given key
 */
export function getScalePitchClasses(key: KeySignature): number[] {
    return SCALE_INTERVALS.map(interval => (key.rootPitchClass + interval) % 12);
}

/**
 * Check if a pitch class is diatonic to the key
 */
export function isDiatonic(pitchClass: number, key: KeySignature): boolean {
    return getScalePitchClasses(key).includes(pitchClass);
}

/**
 * Get the scale degree (1-7) for a pitch class, or null if not in key
 */
export function getScaleDegree(pitchClass: number, key: KeySignature): number | null {
    const scalePitches = getScalePitchClasses(key);
    const index = scalePitches.indexOf(pitchClass);
    return index >= 0 ? index + 1 : null;
}

/**
 * Check if a MIDI note is playable in chord mode for the given key
 */
export function isNotePlayableInKey(midiNote: number, key: KeySignature): boolean {
    const pitchClass = midiNote % 12;
    return isDiatonic(pitchClass, key);
}

/**
 * Get display name for a note appropriate for the key signature
 */
export function getNoteDisplayName(midiNote: number, key: KeySignature): string {
    const pitchClass = midiNote % 12;
    const usesFlats = FLAT_KEYS.includes(key.id);
    return usesFlats ? FLAT_NOTE_NAMES[pitchClass] : NOTE_NAMES[pitchClass];
}

export interface Chord {
    name: string;
    midiNotes: number[];
}

/**
 * Build a chord for a given scale degree in a key
 * @param key - The key signature
 * @param scaleDegree - 1-7 (I through VII)
 * @param chordType - 'triad' or 'seventh'
 * @param octaveBaseMidi - The MIDI note for C in the target octave (default 60 for C4)
 */
export function buildChord(
    key: KeySignature,
    scaleDegree: number,
    chordType: ChordType,
    octaveBaseMidi: number = 60
): Chord {
    const scalePitches = getScalePitchClasses(key);
    const degreeIndex = scaleDegree - 1;

    // Build chord from scale degrees
    // Triad: root, 3rd, 5th (degrees 0, 2, 4)
    // Seventh: root, 3rd, 5th, 7th (degrees 0, 2, 4, 6)
    const chordDegreeOffsets = chordType === 'triad' ? [0, 2, 4] : [0, 2, 4, 6];
    const chordDegrees = chordDegreeOffsets.map(offset => (degreeIndex + offset) % 7);

    // Build MIDI notes ensuring they are in ascending order
    const midiNotes: number[] = [];
    for (const degree of chordDegrees) {
        const pitchClass = scalePitches[degree];
        let midiNote = octaveBaseMidi + pitchClass;

        // Ensure this note is higher than the previous one
        if (midiNotes.length > 0) {
            while (midiNote <= midiNotes[midiNotes.length - 1]) {
                midiNote += 12;
            }
        }
        midiNotes.push(midiNote);
    }

    // Build chord name
    const rootPitchClass = scalePitches[degreeIndex];
    const usesFlats = FLAT_KEYS.includes(key.id);
    const rootName = usesFlats ? FLAT_NOTE_NAMES[rootPitchClass] : NOTE_NAMES[rootPitchClass];
    const quality = chordType === 'triad'
        ? TRIAD_CHORD_QUALITIES[degreeIndex]
        : SEVENTH_CHORD_QUALITIES[degreeIndex];
    const chordName = rootName + quality;

    return { name: chordName, midiNotes };
}

/**
 * Get chord for a MIDI note in the given key
 * Returns null if the note is not diatonic to the key
 */
export function getChordForNote(
    midiNote: number,
    key: KeySignature,
    chordType: ChordType = 'seventh'
): Chord | null {
    const pitchClass = midiNote % 12;
    const scaleDegree = getScaleDegree(pitchClass, key);
    if (scaleDegree === null) return null;

    // Use the actual octave of the pressed note
    const octaveBase = Math.floor(midiNote / 12) * 12;
    return buildChord(key, scaleDegree, chordType, octaveBase);
}

/**
 * Get the notes to play for a MIDI note
 * In single mode: returns just the root note
 * In chord mode: returns the chord notes if diatonic, or single note if not
 */
export function getNotesToPlay(
    midiNote: number,
    chordMode: boolean,
    key: KeySignature,
    chordType: ChordType = 'seventh'
): number[] {
    if (!chordMode) {
        return [midiNote];
    }

    const chord = getChordForNote(midiNote, key, chordType);
    return chord?.midiNotes ?? [midiNote];
}

/**
 * Get the chord name for display when playing a note in chord mode
 */
export function getChordName(
    midiNote: number,
    key: KeySignature,
    chordType: ChordType
): string | null {
    const chord = getChordForNote(midiNote, key, chordType);
    return chord?.name ?? null;
}

/**
 * Convert MIDI note to frequency
 */
export function midiToFrequency(midiNote: number): number {
    return 440 * Math.pow(2, (midiNote - 69) / 12);
}
