/**
 * AudioContext Manager for Synth.io Web
 * Handles AudioContext lifecycle, worklet loading, and audio graph setup
 */

let audioContext: AudioContext | null = null;
let isWorkletLoaded = false;

export interface AudioEngineConfig {
  sampleRate?: number;
  latencyHint?: AudioContextLatencyCategory;
}

/**
 * Initialize or resume the AudioContext
 * Must be called from a user gesture (click/touch)
 */
export async function initAudioContext(config: AudioEngineConfig = {}): Promise<AudioContext> {
  if (audioContext && audioContext.state === 'running') {
    return audioContext;
  }

  if (!audioContext) {
    audioContext = new AudioContext({
      sampleRate: config.sampleRate || 48000,
      latencyHint: config.latencyHint || 'interactive',
    });
  }

  // Resume if suspended (browser autoplay policy)
  if (audioContext.state === 'suspended') {
    await audioContext.resume();
  }

  return audioContext;
}

/**
 * Get the current AudioContext (or null if not initialized)
 */
export function getAudioContext(): AudioContext | null {
  return audioContext;
}

/**
 * Load the synthesizer AudioWorklet processor
 */
export async function loadSynthWorklet(ctx: AudioContext): Promise<void> {
  if (isWorkletLoaded) return;

  try {
    await ctx.audioWorklet.addModule('/worklets/synth-processor.js');
    isWorkletLoaded = true;
    console.log('[AudioEngine] Synth worklet loaded successfully');
  } catch (error) {
    console.error('[AudioEngine] Failed to load synth worklet:', error);
    throw error;
  }
}

/**
 * Create and connect the main synth node
 */
export function createSynthNode(ctx: AudioContext): AudioWorkletNode {
  if (!isWorkletLoaded) {
    throw new Error('Synth worklet not loaded. Call loadSynthWorklet first.');
  }

  const synthNode = new AudioWorkletNode(ctx, 'synth-processor', {
    numberOfInputs: 0,
    numberOfOutputs: 1,
    outputChannelCount: [2], // Stereo
    processorOptions: {
      sampleRate: ctx.sampleRate,
    },
  });

  return synthNode;
}

/**
 * Create a gain node for master volume
 */
export function createMasterGain(ctx: AudioContext, initialVolume = 0.7): GainNode {
  const gain = ctx.createGain();
  gain.gain.value = initialVolume;
  gain.connect(ctx.destination);
  return gain;
}

/**
 * Create audio effects chain
 */
export function createEffectsChain(ctx: AudioContext) {
  // Delay Effect
  const delayNode = ctx.createDelay(2.0);
  const delayFeedback = ctx.createGain();
  const delayWet = ctx.createGain();
  delayNode.delayTime.value = 0.3;
  delayFeedback.gain.value = 0.3;
  delayWet.gain.value = 0;

  delayNode.connect(delayFeedback);
  delayFeedback.connect(delayNode);
  delayNode.connect(delayWet);

  // Filter
  const filter = ctx.createBiquadFilter();
  filter.type = 'lowpass';
  filter.frequency.value = 8000;
  filter.Q.value = 1;

  return {
    delay: { node: delayNode, feedback: delayFeedback, wet: delayWet },
    filter,
  };
}

/**
 * Check if Web Audio API is supported
 */
export function isWebAudioSupported(): boolean {
  return typeof AudioContext !== 'undefined' || typeof (window as any).webkitAudioContext !== 'undefined';
}

/**
 * Clean up audio resources
 */
export async function disposeAudioContext(): Promise<void> {
  if (audioContext) {
    await audioContext.close();
    audioContext = null;
    isWorkletLoaded = false;
  }
}
