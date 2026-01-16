'use client';

/**
 * Piano Keyboard Component
 * Touch-enabled, responsive keyboard with chord mode and proper black key alignment
 * Matches mobile app layout from Keyboard.kt
 */

import { useCallback, useRef, useEffect, useState } from 'react';
import { useSynthStore } from '@/stores/synth-store';
import {
    getKeySignature,
    isNotePlayableInKey,
    getNotesToPlay,
    getChordName,
    getNoteDisplayName,
} from '@/lib/music-constants';

interface KeyboardProps {
    className?: string;
}

// White keys in one octave (C to B)
const WHITE_KEYS_IN_OCTAVE = [0, 2, 4, 5, 7, 9, 11]; // C, D, E, F, G, A, B pitch classes
const WHITE_KEY_NAMES = ['C', 'D', 'E', 'F', 'G', 'A', 'B'];

// Black keys with their position relative to white keys
// Position is percentage from left edge of the white key they're next to
const BLACK_KEY_CONFIG = [
    { pitchClass: 1, afterWhiteIndex: 0, position: 0.6 },  // C#, 60% across C key
    { pitchClass: 3, afterWhiteIndex: 1, position: 0.6 },  // D#, 60% across D key
    // No black key after E (index 2)
    { pitchClass: 6, afterWhiteIndex: 3, position: 0.6 },  // F#, 60% across F key
    { pitchClass: 8, afterWhiteIndex: 4, position: 0.6 },  // G#, 60% across G key
    { pitchClass: 10, afterWhiteIndex: 5, position: 0.6 }, // A#, 60% across A key
];

// Calculate MIDI note from octave and pitch class
function getMidiNote(octave: number, pitchClass: number): number {
    return octave * 12 + pitchClass + 12; // MIDI C0 = 12
}

export default function Keyboard({ className = '' }: KeyboardProps) {
    const {
        octave,
        activeNotes,
        noteOn,
        noteOff,
        setOctave,
        isAudioInitialized,
        isChordMode,
        keySignature: keySignatureId,
        chordType,
        setCurrentChordName,
    } = useSynthStore();

    const synthNodeRef = useRef<AudioWorkletNode | null>(null);
    const activeNotesMapRef = useRef<Map<number, number[]>>(new Map());
    const [pressedVisualKeys, setPressedVisualKeys] = useState<Set<number>>(new Set());

    const keySignature = getKeySignature(keySignatureId);

    // Send note messages to AudioWorklet
    const sendNoteOn = useCallback((midiNote: number) => {
        const notesToPlay = getNotesToPlay(midiNote, isChordMode, keySignature, chordType);

        // Store the notes being played for this key
        activeNotesMapRef.current.set(midiNote, notesToPlay);

        // Update chord name display
        if (isChordMode) {
            const chordName = getChordName(midiNote, keySignature, chordType);
            setCurrentChordName(chordName);
        }

        // Play all notes
        notesToPlay.forEach((note) => {
            noteOn(note);
            synthNodeRef.current?.port.postMessage({
                type: 'noteOn',
                data: { note, velocity: 1 },
            });
        });
    }, [isChordMode, keySignature, chordType, noteOn, setCurrentChordName]);

    const sendNoteOff = useCallback((midiNote: number) => {
        const notesToStop = activeNotesMapRef.current.get(midiNote) || [midiNote];

        // Clear the stored notes
        activeNotesMapRef.current.delete(midiNote);

        // Clear chord name if no more notes
        if (activeNotesMapRef.current.size === 0) {
            setCurrentChordName(null);
        }

        // Stop all notes
        notesToStop.forEach((note) => {
            noteOff(note);
            synthNodeRef.current?.port.postMessage({
                type: 'noteOff',
                data: { note },
            });
        });
    }, [noteOff, setCurrentChordName]);

    // Handle key press
    const handleKeyPress = useCallback((midiNote: number) => {
        // Check if playable in chord mode
        if (isChordMode && !isNotePlayableInKey(midiNote, keySignature)) {
            return;
        }

        if (!pressedVisualKeys.has(midiNote)) {
            setPressedVisualKeys((prev) => new Set(prev).add(midiNote));
            sendNoteOn(midiNote);
        }
    }, [isChordMode, keySignature, pressedVisualKeys, sendNoteOn]);

    const handleKeyRelease = useCallback((midiNote: number) => {
        if (pressedVisualKeys.has(midiNote)) {
            setPressedVisualKeys((prev) => {
                const next = new Set(prev);
                next.delete(midiNote);
                return next;
            });
            sendNoteOff(midiNote);
        }
    }, [pressedVisualKeys, sendNoteOff]);

    // Handle pointer events
    const handlePointerDown = useCallback((midiNote: number, e: React.PointerEvent) => {
        e.preventDefault();
        e.stopPropagation();
        (e.target as HTMLElement).setPointerCapture(e.pointerId);
        handleKeyPress(midiNote);
    }, [handleKeyPress]);

    const handlePointerUp = useCallback((midiNote: number, e: React.PointerEvent) => {
        e.preventDefault();
        handleKeyRelease(midiNote);
    }, [handleKeyRelease]);

    // Clean up on unmount
    useEffect(() => {
        return () => {
            pressedVisualKeys.forEach(note => sendNoteOff(note));
        };
    }, [pressedVisualKeys, sendNoteOff]);

    // Store synth node ref
    useEffect(() => {
        const handleSynthNode = (node: AudioWorkletNode) => {
            synthNodeRef.current = node;
        };
        (window as any).__setSynthNode = handleSynthNode;
        return () => {
            (window as any).__setSynthNode = undefined;
        };
    }, []);

    // Render a single octave
    const renderOctave = (oct: number) => {
        const whiteKeyWidth = 100 / 7; // 7 white keys per octave

        return (
            <div key={`octave-${oct}`} className="relative flex-1 h-full">
                {/* White keys */}
                <div className="flex h-full gap-[2px]">
                    {WHITE_KEYS_IN_OCTAVE.map((pitchClass, index) => {
                        const midiNote = getMidiNote(oct, pitchClass);
                        const isPressed = pressedVisualKeys.has(midiNote);
                        const isPlayable = !isChordMode || isNotePlayableInKey(midiNote, keySignature);
                        const noteName = WHITE_KEY_NAMES[index];

                        return (
                            <div
                                key={`white-${oct}-${index}`}
                                className={`
                                  flex-1 h-full rounded-b-xl cursor-pointer select-none
                                  transition-all duration-75 border-b-4
                                  ${isPressed
                                        ? 'translate-y-[2px] shadow-sm border-b-2'
                                        : 'shadow-md'
                                    }
                                  ${!isPlayable ? 'opacity-40' : ''}
                                `}
                                style={{
                                    background: isPressed
                                        ? 'var(--white-key-pressed)'
                                        : 'linear-gradient(to bottom, var(--white-key-default), color-mix(in srgb, var(--white-key-default) 90%, #ccc))',
                                    borderColor: 'color-mix(in srgb, var(--white-key-default) 70%, #999)',
                                }}
                                onPointerDown={(e) => handlePointerDown(midiNote, e)}
                                onPointerUp={(e) => handlePointerUp(midiNote, e)}
                                onPointerLeave={(e) => e.buttons > 0 && handleKeyRelease(midiNote)}
                                onContextMenu={(e) => e.preventDefault()}
                            >
                                <div className="h-full flex flex-col justify-end items-center pb-2">
                                    {isPlayable && (
                                        <span className="text-[10px] text-gray-500 select-none">
                                            {noteName}{oct}
                                        </span>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* Black keys - positioned absolutely */}
                <div className="absolute top-0 left-0 right-0 h-[60%] pointer-events-none">
                    {BLACK_KEY_CONFIG.map(({ pitchClass, afterWhiteIndex, position }) => {
                        const midiNote = getMidiNote(oct, pitchClass);
                        const isPressed = pressedVisualKeys.has(midiNote);
                        const isPlayable = !isChordMode || isNotePlayableInKey(midiNote, keySignature);

                        // Calculate position: center of black key should be at position% across the gap between white keys
                        const leftPosition = ((afterWhiteIndex + position) / 7) * 100;

                        return (
                            <div
                                key={`black-${oct}-${pitchClass}`}
                                className={`
                                  absolute w-[10%] h-full rounded-b-lg cursor-pointer select-none pointer-events-auto
                                  transition-all duration-75 border-b-4
                                  ${isPressed
                                        ? 'translate-y-[2px] border-b-2'
                                        : 'shadow-lg'
                                    }
                                  ${!isPlayable ? 'opacity-30' : ''}
                                `}
                                style={{
                                    left: `${leftPosition}%`,
                                    transform: 'translateX(-50%)',
                                    background: isPressed
                                        ? 'var(--black-key-pressed)'
                                        : 'linear-gradient(to bottom, var(--black-key-default), color-mix(in srgb, var(--black-key-default) 70%, black))',
                                    borderColor: '#000',
                                }}
                                onPointerDown={(e) => handlePointerDown(midiNote, e)}
                                onPointerUp={(e) => handlePointerUp(midiNote, e)}
                                onPointerLeave={(e) => e.buttons > 0 && handleKeyRelease(midiNote)}
                                onContextMenu={(e) => e.preventDefault()}
                            />
                        );
                    })}
                </div>
            </div>
        );
    };

    return (
        <div className={`flex flex-col gap-3 ${className}`}>
            {/* Octave controls */}
            <div className="flex items-center justify-center gap-4">
                <button
                    onClick={() => setOctave(octave - 1)}
                    disabled={octave <= 1}
                    className="px-4 py-2 rounded-lg bg-[var(--surface)] text-[var(--foreground)] border border-[var(--pastel-lavender)] disabled:opacity-50 hover:bg-[var(--pastel-lavender)] transition-colors"
                >
                    Octave −
                </button>
                <span className="text-lg font-medium text-[var(--foreground)]">
                    C{octave} - C{octave + 2}
                </span>
                <button
                    onClick={() => setOctave(octave + 1)}
                    disabled={octave >= 6}
                    className="px-4 py-2 rounded-lg bg-[var(--surface)] text-[var(--foreground)] border border-[var(--pastel-lavender)] disabled:opacity-50 hover:bg-[var(--pastel-lavender)] transition-colors"
                >
                    Octave +
                </button>
            </div>

            {/* Keyboard container */}
            <div
                className="flex w-full h-40 rounded-lg overflow-hidden shadow-lg bg-[var(--surface)] p-1"
                style={{ touchAction: 'none' }}
            >
                {renderOctave(octave)}
                {renderOctave(octave + 1)}
            </div>

            {!isAudioInitialized && (
                <p className="text-center text-sm text-[var(--text-secondary)]">
                    Tap anywhere to enable audio
                </p>
            )}
        </div>
    );
}
