'use client';

/**
 * Synth.io Main Page
 * Audio-enabled synthesizer interface with full controls
 */

import { useState, useCallback, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { useSynthStore } from '@/stores/synth-store';
import { initAudioContext, loadSynthWorklet, createSynthNode, createMasterGain, isWebAudioSupported } from '@/audio/engine';
import CollapsibleSection from '@/components/controls/CollapsibleSection';
import ADSRPanel from '@/components/controls/ADSRPanel';
import FilterPanel from '@/components/controls/FilterPanel';
import LFOPanel from '@/components/controls/LFOPanel';
import EffectsPanel from '@/components/controls/EffectsPanel';
import ChordModePanel from '@/components/controls/ChordModePanel';

// Dynamically import Keyboard to avoid SSR issues with pointer events
const Keyboard = dynamic(() => import('@/components/keyboard/Keyboard'), { ssr: false });

export default function Home() {
  const {
    isAudioInitialized,
    setAudioInitialized,
    masterVolume,
    setMasterVolume,
    waveform,
    setWaveform,
    isDarkMode,
    toggleDarkMode,
    detune,
    setDetune,
  } = useSynthStore();

  const [audioError, setAudioError] = useState<string | null>(null);
  const [synthNode, setSynthNode] = useState<AudioWorkletNode | null>(null);
  const [masterGain, setMasterGain] = useState<GainNode | null>(null);

  // Initialize audio on first user interaction
  const initAudio = useCallback(async () => {
    if (isAudioInitialized) return;

    try {
      if (!isWebAudioSupported()) {
        throw new Error('Web Audio API not supported in this browser');
      }

      const ctx = await initAudioContext({ sampleRate: 48000 });
      await loadSynthWorklet(ctx);

      const synth = createSynthNode(ctx);
      const master = createMasterGain(ctx, masterVolume);

      synth.connect(master);

      setSynthNode(synth);
      setMasterGain(master);
      setAudioInitialized(true);

      // Expose synth node to keyboard component
      if ((window as any).__setSynthNode) {
        (window as any).__setSynthNode(synth);
      }

      console.log('[Synth.io] Audio initialized successfully');
    } catch (error) {
      console.error('[Synth.io] Audio initialization failed:', error);
      setAudioError(error instanceof Error ? error.message : 'Unknown error');
    }
  }, [isAudioInitialized, masterVolume, setAudioInitialized]);

  // Update master volume
  useEffect(() => {
    if (masterGain) {
      masterGain.gain.setValueAtTime(masterVolume, masterGain.context.currentTime);
    }
  }, [masterVolume, masterGain]);

  // Update waveform
  useEffect(() => {
    if (synthNode) {
      synthNode.port.postMessage({
        type: 'setParam',
        data: { param: 'waveform', value: waveform },
      });
    }
  }, [waveform, synthNode]);

  // Apply dark mode class
  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  return (
    <main
      className="min-h-screen flex flex-col p-4 md:p-6 gap-4"
      onClick={initAudio}
      onTouchStart={initAudio}
    >
      {/* Header */}
      <header className="flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl md:text-3xl font-bold bg-gradient-to-r from-[var(--pastel-pink)] to-[var(--pastel-lavender)] bg-clip-text text-transparent">
            Synth.io
          </h1>
          {isAudioInitialized && (
            <span className="px-2 py-1 text-xs rounded-full bg-[var(--pastel-mint)] text-green-800">
              Audio Active
            </span>
          )}
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={(e) => { e.stopPropagation(); toggleDarkMode(); }}
            className="p-2 rounded-lg bg-[var(--surface)] hover:bg-[var(--pastel-lavender)] transition-colors"
            aria-label="Toggle dark mode"
          >
            {isDarkMode ? '☀️' : '🌙'}
          </button>
        </div>
      </header>

      {/* Error message */}
      {audioError && (
        <div className="p-4 rounded-lg bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200 shrink-0">
          {audioError}
        </div>
      )}

      {/* Scrollable Controls Area */}
      <div className="flex-1 overflow-y-auto flex flex-col lg:flex-row gap-4">
        {/* Left Column: Oscillator & Effects */}
        <div className="flex-1 flex flex-col gap-4">
          {/* Oscillator Section */}
          <CollapsibleSection title="Oscillator" accentColor="var(--pastel-pink)">
            <div className="space-y-4">
              {/* Waveform Selector */}
              <div>
                <label className="text-xs text-[var(--text-secondary)] block mb-2">Waveform</label>
                <div className="flex gap-2">
                  {(['sine', 'square', 'sawtooth', 'triangle'] as const).map((w) => (
                    <button
                      key={w}
                      onClick={(e) => { e.stopPropagation(); setWaveform(w); }}
                      className={`
                        flex-1 py-2 px-2 rounded-lg text-xs md:text-sm capitalize transition-all
                        ${waveform === w
                          ? 'bg-[var(--pastel-pink)] text-gray-800 shadow-sm'
                          : 'bg-[var(--background)] text-[var(--text-secondary)] hover:bg-[var(--pastel-lavender)]'
                        }
                      `}
                    >
                      {w}
                    </button>
                  ))}
                </div>
              </div>

              {/* Detune */}
              <div>
                <label className="text-xs text-[var(--text-secondary)] block mb-2">
                  Detune: {detune > 0 ? '+' : ''}{detune} cents
                </label>
                <input
                  type="range"
                  min="-100"
                  max="100"
                  step="1"
                  value={detune}
                  onChange={(e) => setDetune(parseInt(e.target.value))}
                  onClick={(e) => e.stopPropagation()}
                  className="w-full"
                />
              </div>

              {/* Master Volume */}
              <div>
                <label className="text-xs text-[var(--text-secondary)] block mb-2">
                  Master Volume: {Math.round(masterVolume * 100)}%
                </label>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  value={masterVolume}
                  onChange={(e) => setMasterVolume(parseFloat(e.target.value))}
                  onClick={(e) => e.stopPropagation()}
                  className="w-full"
                />
              </div>
            </div>
          </CollapsibleSection>

          {/* Filter Section */}
          <CollapsibleSection title="Filter" accentColor="var(--pastel-blue)">
            <FilterPanel synthNode={synthNode} />
          </CollapsibleSection>

          {/* LFO Section */}
          <CollapsibleSection title="LFO" accentColor="var(--pastel-yellow)" defaultOpen={false}>
            <LFOPanel synthNode={synthNode} />
          </CollapsibleSection>

          {/* Effects Section */}
          <CollapsibleSection title="Effects" accentColor="var(--pastel-coral)" defaultOpen={false}>
            <EffectsPanel />
          </CollapsibleSection>
        </div>

        {/* Right Column: Envelope */}
        <div className="lg:w-64 flex flex-col gap-4">
          <CollapsibleSection title="Envelope (ADSR)" accentColor="var(--pastel-lavender)">
            <ADSRPanel synthNode={synthNode} />
          </CollapsibleSection>

          {/* Chord Mode Section */}
          <CollapsibleSection title="Chord Mode" accentColor="var(--pastel-mint)" defaultOpen={true}>
            <ChordModePanel />
          </CollapsibleSection>

          {/* Status Card */}
          <div className="p-4 rounded-xl bg-[var(--surface)] shadow-md flex-1 flex flex-col justify-center items-center gap-2">
            {!isAudioInitialized ? (
              <>
                <div className="w-12 h-12 rounded-full bg-[var(--pastel-peach)] flex items-center justify-center text-2xl">
                  🎹
                </div>
                <p className="text-sm text-[var(--text-secondary)] text-center">
                  Tap anywhere to enable audio
                </p>
              </>
            ) : (
              <>
                <div className="w-12 h-12 rounded-full bg-[var(--pastel-mint)] flex items-center justify-center text-2xl">
                  ✓
                </div>
                <p className="text-sm text-[var(--text-secondary)] text-center">
                  Play the keyboard below!
                </p>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Piano Keyboard - Fixed at bottom */}
      <section className="shrink-0 pt-2">
        <Keyboard className="w-full max-w-4xl mx-auto" />
      </section>

      {/* Footer */}
      <footer className="text-center text-xs text-[var(--text-secondary)] shrink-0">
        Web Synthesizer • Built with Next.js & Web Audio API
      </footer>
    </main>
  );
}
