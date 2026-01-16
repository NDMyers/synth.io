'use client';

/**
 * Effects Control Panel
 * Delay, Reverb, and Tremolo controls
 */

import { useSynthStore } from '@/stores/synth-store';
import Knob from './Knob';
import Slider from './Slider';

interface EffectsPanelProps {
    delayNode?: DelayNode | null;
    delayFeedbackGain?: GainNode | null;
    delayWetGain?: GainNode | null;
    tremoloGain?: GainNode | null;
    tremoloLfo?: OscillatorNode | null;
}

export default function EffectsPanel({
    delayNode,
    delayFeedbackGain,
    delayWetGain,
}: EffectsPanelProps) {
    const {
        delayTime,
        setDelayTime,
        delayFeedback,
        setDelayFeedback,
        delayMix,
        setDelayMix,
        reverbMix,
        setReverbMix,
        tremoloRate,
        setTremoloRate,
        tremoloDepth,
        setTremoloDepth,
    } = useSynthStore();

    // Update delay parameters in real-time
    const updateDelayTime = (time: number) => {
        setDelayTime(time);
        if (delayNode) {
            delayNode.delayTime.setValueAtTime(time, delayNode.context.currentTime);
        }
    };

    const updateDelayFeedback = (feedback: number) => {
        setDelayFeedback(feedback);
        if (delayFeedbackGain) {
            delayFeedbackGain.gain.setValueAtTime(feedback, delayFeedbackGain.context.currentTime);
        }
    };

    const updateDelayMix = (mix: number) => {
        setDelayMix(mix);
        if (delayWetGain) {
            delayWetGain.gain.setValueAtTime(mix, delayWetGain.context.currentTime);
        }
    };

    return (
        <div className="space-y-6">
            {/* Delay Section */}
            <div>
                <h3 className="text-xs font-medium text-[var(--text-secondary)] mb-3 flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[var(--pastel-blue)]" />
                    Delay
                </h3>
                <div className="flex justify-around items-center gap-3">
                    <Knob
                        value={delayTime}
                        min={0.01}
                        max={1}
                        step={0.01}
                        label="Time"
                        unit="s"
                        color="var(--pastel-blue)"
                        onChange={updateDelayTime}
                        size="sm"
                    />
                    <Knob
                        value={delayFeedback}
                        min={0}
                        max={0.9}
                        step={0.01}
                        label="Feedback"
                        unit=""
                        color="var(--pastel-lavender)"
                        onChange={updateDelayFeedback}
                        size="sm"
                    />
                    <Knob
                        value={delayMix}
                        min={0}
                        max={1}
                        step={0.01}
                        label="Mix"
                        unit=""
                        color="var(--pastel-mint)"
                        onChange={updateDelayMix}
                        size="sm"
                    />
                </div>
            </div>

            {/* Reverb Section */}
            <div>
                <h3 className="text-xs font-medium text-[var(--text-secondary)] mb-3 flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[var(--pastel-coral)]" />
                    Reverb
                </h3>
                <div className="flex justify-center">
                    <Slider
                        value={reverbMix}
                        min={0}
                        max={1}
                        step={0.01}
                        label="Mix"
                        unit=""
                        color="var(--pastel-coral)"
                        onChange={setReverbMix}
                    />
                </div>
                <p className="text-xs text-[var(--text-secondary)] text-center mt-2 opacity-60">
                    (Coming soon - impulse response)
                </p>
            </div>

            {/* Tremolo Section */}
            <div>
                <h3 className="text-xs font-medium text-[var(--text-secondary)] mb-3 flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[var(--pastel-yellow)]" />
                    Tremolo
                </h3>
                <div className="flex justify-around items-center gap-3">
                    <Knob
                        value={tremoloRate}
                        min={0.5}
                        max={20}
                        step={0.1}
                        label="Rate"
                        unit="Hz"
                        color="var(--pastel-yellow)"
                        onChange={setTremoloRate}
                        size="sm"
                    />
                    <Knob
                        value={tremoloDepth}
                        min={0}
                        max={1}
                        step={0.01}
                        label="Depth"
                        unit=""
                        color="var(--pastel-peach)"
                        onChange={setTremoloDepth}
                        size="sm"
                    />
                </div>
            </div>
        </div>
    );
}
