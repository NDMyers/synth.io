'use client';

/**
 * Chord Mode Control Panel
 * Single/Chord toggle, Key signature selector, Chord type selector
 * Matches SideMenu.kt controls from Android
 */

import { useSynthStore } from '@/stores/synth-store';
import { KEY_SIGNATURES } from '@/lib/music-constants';

export default function ChordModePanel() {
    const {
        isChordMode,
        toggleChordMode,
        keySignature,
        setKeySignature,
        chordType,
        setChordType,
        currentChordName,
    } = useSynthStore();

    return (
        <div className="space-y-4">
            {/* Single / Chord Toggle */}
            <div className="flex items-center justify-center gap-3">
                <span
                    className={`text-sm font-medium ${!isChordMode ? 'text-[var(--pastel-pink)]' : 'text-[var(--text-secondary)]'}`}
                >
                    Single
                </span>
                <button
                    onClick={(e) => { e.stopPropagation(); toggleChordMode(); }}
                    className={`
            relative w-14 h-7 rounded-full transition-colors
            ${isChordMode ? 'bg-[var(--pastel-pink)]' : 'bg-[var(--background-light)]'}
          `}
                >
                    <div
                        className={`
              absolute top-1 w-5 h-5 rounded-full bg-white shadow-md transition-transform
              ${isChordMode ? 'translate-x-8' : 'translate-x-1'}
            `}
                    />
                </button>
                <span
                    className={`text-sm font-medium ${isChordMode ? 'text-[var(--pastel-pink)]' : 'text-[var(--text-secondary)]'}`}
                >
                    Chord
                </span>
            </div>

            {/* Chord Display */}
            {isChordMode && currentChordName && (
                <div className="text-center">
                    <span className="text-2xl font-bold text-[var(--pastel-pink)]">
                        {currentChordName}
                    </span>
                </div>
            )}

            {/* Controls visible in chord mode */}
            {isChordMode && (
                <>
                    {/* Chord Type Toggle */}
                    <div>
                        <label className="text-xs text-[var(--text-secondary)] block mb-2">Chord Type</label>
                        <div className="flex gap-2">
                            <button
                                onClick={(e) => { e.stopPropagation(); setChordType('triad'); }}
                                className={`
                  flex-1 py-2 px-3 rounded-lg text-sm transition-all
                  ${chordType === 'triad'
                                        ? 'bg-[var(--pastel-lavender)] text-gray-800 shadow-sm'
                                        : 'bg-[var(--background)] text-[var(--text-secondary)] hover:bg-[var(--pastel-lavender)]/50'
                                    }
                `}
                            >
                                Triads
                            </button>
                            <button
                                onClick={(e) => { e.stopPropagation(); setChordType('seventh'); }}
                                className={`
                  flex-1 py-2 px-3 rounded-lg text-sm transition-all
                  ${chordType === 'seventh'
                                        ? 'bg-[var(--pastel-lavender)] text-gray-800 shadow-sm'
                                        : 'bg-[var(--background)] text-[var(--text-secondary)] hover:bg-[var(--pastel-lavender)]/50'
                                    }
                `}
                            >
                                7th Chords
                            </button>
                        </div>
                    </div>

                    {/* Key Signature Grid */}
                    <div>
                        <label className="text-xs text-[var(--text-secondary)] block mb-2">
                            Key: {KEY_SIGNATURES.find(k => k.id === keySignature)?.displayName}
                        </label>
                        <div className="grid grid-cols-4 gap-1">
                            {KEY_SIGNATURES.map((key) => (
                                <button
                                    key={key.id}
                                    onClick={(e) => { e.stopPropagation(); setKeySignature(key.id); }}
                                    className={`
                    py-1.5 px-1 rounded-md text-xs font-medium transition-all
                    ${keySignature === key.id
                                            ? 'bg-[var(--pastel-pink)] text-gray-800 shadow-sm'
                                            : 'bg-[var(--background)] text-[var(--text-secondary)] hover:bg-[var(--pastel-pink)]/50'
                                        }
                  `}
                                >
                                    {key.displayName.split('/')[0]}
                                </button>
                            ))}
                        </div>
                    </div>
                </>
            )}

            {/* Help text */}
            {!isChordMode && (
                <p className="text-xs text-[var(--text-secondary)] text-center opacity-60">
                    Enable Chord mode to play chords with single keys
                </p>
            )}
        </div>
    );
}
