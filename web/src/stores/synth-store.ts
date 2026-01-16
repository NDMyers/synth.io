/**
 * Synth.io Global State Store (Zustand)
 * Manages synthesizer parameters, looper state, and UI state
 */

import { create } from 'zustand';
import type { KeySignatureId, ChordType } from '@/lib/music-constants';

// Waveform types matching Android
export type Waveform = 'sine' | 'square' | 'sawtooth' | 'triangle';

// Re-export for convenience
export type { KeySignatureId, ChordType };

// Looper state
export type LooperState = 'idle' | 'pre_count' | 'recording' | 'playing' | 'stopped';

// Track state
export interface LoopTrack {
    hasContent: boolean;
    volume: number;
    isMuted: boolean;
    isSolo: boolean;
}

// Export job state
export type ExportStatus = 'pending' | 'mixing' | 'encoding' | 'complete' | 'failed';

export interface ExportJob {
    id: string;
    filename: string;
    trackMask: number;
    includeDrums: boolean;
    quality: 'high_quality' | 'compressed';
    status: ExportStatus;
    progress: number;
    outputUrl?: string;
    createdAt: number;
}

// Main synth store
interface SynthStore {
    // Audio State
    isAudioInitialized: boolean;
    setAudioInitialized: (initialized: boolean) => void;

    // Master Controls
    masterVolume: number;
    setMasterVolume: (volume: number) => void;

    // Oscillator
    waveform: Waveform;
    setWaveform: (waveform: Waveform) => void;
    octave: number;
    setOctave: (octave: number) => void;
    detune: number;
    setDetune: (detune: number) => void;

    // Filter
    filterCutoff: number;
    setFilterCutoff: (cutoff: number) => void;
    filterResonance: number;
    setFilterResonance: (resonance: number) => void;

    // ADSR Envelope
    attackTime: number;
    setAttackTime: (time: number) => void;
    decayTime: number;
    setDecayTime: (time: number) => void;
    sustainLevel: number;
    setSustainLevel: (level: number) => void;
    releaseTime: number;
    setReleaseTime: (time: number) => void;

    // LFO
    lfoRate: number;
    setLfoRate: (rate: number) => void;
    lfoAmount: number;
    setLfoAmount: (amount: number) => void;

    // Effects
    delayTime: number;
    setDelayTime: (time: number) => void;
    delayFeedback: number;
    setDelayFeedback: (feedback: number) => void;
    delayMix: number;
    setDelayMix: (mix: number) => void;
    reverbMix: number;
    setReverbMix: (mix: number) => void;
    tremoloRate: number;
    setTremoloRate: (rate: number) => void;
    tremoloDepth: number;
    setTremoloDepth: (depth: number) => void;

    // Looper
    looperState: LooperState;
    setLooperState: (state: LooperState) => void;
    loopTracks: LoopTrack[];
    updateLoopTrack: (index: number, updates: Partial<LoopTrack>) => void;
    currentBeat: number;
    setCurrentBeat: (beat: number) => void;
    currentBar: number;
    setCurrentBar: (bar: number) => void;
    bpm: number;
    setBpm: (bpm: number) => void;
    barCount: number;
    setBarCount: (count: number) => void;
    metronomeVolume: number;
    setMetronomeVolume: (volume: number) => void;

    // Active Notes (for keyboard display)
    activeNotes: Set<number>;
    noteOn: (midiNote: number) => void;
    noteOff: (midiNote: number) => void;

    // Chord Mode
    isChordMode: boolean;
    setChordMode: (enabled: boolean) => void;
    toggleChordMode: () => void;
    keySignature: KeySignatureId;
    setKeySignature: (key: KeySignatureId) => void;
    chordType: ChordType;
    setChordType: (type: ChordType) => void;
    currentChordName: string | null;
    setCurrentChordName: (name: string | null) => void;

    // UI State
    isDarkMode: boolean;
    toggleDarkMode: () => void;
    showLooperModal: boolean;
    setShowLooperModal: (show: boolean) => void;
    showExportModal: boolean;
    setShowExportModal: (show: boolean) => void;

    // Exports
    exportJobs: ExportJob[];
    addExportJob: (job: ExportJob) => void;
    updateExportJob: (id: string, updates: Partial<ExportJob>) => void;
    removeExportJob: (id: string) => void;

    // MIDI
    isMidiConnected: boolean;
    setMidiConnected: (connected: boolean) => void;
    midiDeviceName: string | null;
    setMidiDeviceName: (name: string | null) => void;
}

export const useSynthStore = create<SynthStore>((set, get) => ({
    // Audio State
    isAudioInitialized: false,
    setAudioInitialized: (initialized) => set({ isAudioInitialized: initialized }),

    // Master Controls
    masterVolume: 0.7,
    setMasterVolume: (volume) => set({ masterVolume: volume }),

    // Oscillator
    waveform: 'sawtooth',
    setWaveform: (waveform) => set({ waveform }),
    octave: 4,
    setOctave: (octave) => set({ octave: Math.max(0, Math.min(8, octave)) }),
    detune: 0,
    setDetune: (detune) => set({ detune }),

    // Filter
    filterCutoff: 8000,
    setFilterCutoff: (cutoff) => set({ filterCutoff: cutoff }),
    filterResonance: 1,
    setFilterResonance: (resonance) => set({ filterResonance: resonance }),

    // ADSR Envelope
    attackTime: 0.01,
    setAttackTime: (time) => set({ attackTime: time }),
    decayTime: 0.1,
    setDecayTime: (time) => set({ decayTime: time }),
    sustainLevel: 0.7,
    setSustainLevel: (level) => set({ sustainLevel: level }),
    releaseTime: 0.3,
    setReleaseTime: (time) => set({ releaseTime: time }),

    // LFO
    lfoRate: 4,
    setLfoRate: (rate) => set({ lfoRate: rate }),
    lfoAmount: 0,
    setLfoAmount: (amount) => set({ lfoAmount: amount }),

    // Effects
    delayTime: 0.3,
    setDelayTime: (time) => set({ delayTime: time }),
    delayFeedback: 0.3,
    setDelayFeedback: (feedback) => set({ delayFeedback: feedback }),
    delayMix: 0,
    setDelayMix: (mix) => set({ delayMix: mix }),
    reverbMix: 0,
    setReverbMix: (mix) => set({ reverbMix: mix }),
    tremoloRate: 4,
    setTremoloRate: (rate) => set({ tremoloRate: rate }),
    tremoloDepth: 0,
    setTremoloDepth: (depth) => set({ tremoloDepth: depth }),

    // Looper
    looperState: 'idle',
    setLooperState: (state) => set({ looperState: state }),
    loopTracks: [
        { hasContent: false, volume: 0.7, isMuted: false, isSolo: false },
        { hasContent: false, volume: 0.7, isMuted: false, isSolo: false },
        { hasContent: false, volume: 0.7, isMuted: false, isSolo: false },
        { hasContent: false, volume: 0.7, isMuted: false, isSolo: false },
    ],
    updateLoopTrack: (index, updates) =>
        set((state) => ({
            loopTracks: state.loopTracks.map((track, i) =>
                i === index ? { ...track, ...updates } : track
            ),
        })),
    currentBeat: 0,
    setCurrentBeat: (beat) => set({ currentBeat: beat }),
    currentBar: 0,
    setCurrentBar: (bar) => set({ currentBar: bar }),
    bpm: 120,
    setBpm: (bpm) => set({ bpm: Math.max(40, Math.min(240, bpm)) }),
    barCount: 4,
    setBarCount: (count) => set({ barCount: Math.max(1, Math.min(8, count)) }),
    metronomeVolume: 0.5,
    setMetronomeVolume: (volume) => set({ metronomeVolume: volume }),

    // Active Notes
    activeNotes: new Set(),
    noteOn: (midiNote) =>
        set((state) => ({
            activeNotes: new Set(state.activeNotes).add(midiNote),
        })),
    noteOff: (midiNote) =>
        set((state) => {
            const notes = new Set(state.activeNotes);
            notes.delete(midiNote);
            return { activeNotes: notes };
        }),

    // Chord Mode
    isChordMode: false,
    setChordMode: (enabled) => set({ isChordMode: enabled }),
    toggleChordMode: () => set((state) => ({ isChordMode: !state.isChordMode })),
    keySignature: 'C',
    setKeySignature: (key) => set({ keySignature: key }),
    chordType: 'seventh',
    setChordType: (type) => set({ chordType: type }),
    currentChordName: null,
    setCurrentChordName: (name) => set({ currentChordName: name }),

    // UI State
    isDarkMode: false,
    toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
    showLooperModal: false,
    setShowLooperModal: (show) => set({ showLooperModal: show }),
    showExportModal: false,
    setShowExportModal: (show) => set({ showExportModal: show }),

    // Exports
    exportJobs: [],
    addExportJob: (job) =>
        set((state) => ({ exportJobs: [job, ...state.exportJobs] })),
    updateExportJob: (id, updates) =>
        set((state) => ({
            exportJobs: state.exportJobs.map((job) =>
                job.id === id ? { ...job, ...updates } : job
            ),
        })),
    removeExportJob: (id) =>
        set((state) => ({
            exportJobs: state.exportJobs.filter((job) => job.id !== id),
        })),

    // MIDI
    isMidiConnected: false,
    setMidiConnected: (connected) => set({ isMidiConnected: connected }),
    midiDeviceName: null,
    setMidiDeviceName: (name) => set({ midiDeviceName: name }),
}));
