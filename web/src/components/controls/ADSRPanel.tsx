'use client';

/**
 * ADSR Envelope Control Panel
 * Attack, Decay, Sustain, Release sliders
 */

import { useSynthStore } from '@/stores/synth-store';
import Slider from './Slider';

interface ADSRPanelProps {
    synthNode: AudioWorkletNode | null;
}

export default function ADSRPanel({ synthNode }: ADSRPanelProps) {
    const {
        attackTime,
        setAttackTime,
        decayTime,
        setDecayTime,
        sustainLevel,
        setSustainLevel,
        releaseTime,
        setReleaseTime,
    } = useSynthStore();

    const updateParam = (param: string, value: number, setter: (v: number) => void) => {
        setter(value);
        synthNode?.port.postMessage({
            type: 'setParam',
            data: { param, value },
        });
    };

    return (
        <div className="flex justify-around items-end gap-2 py-2">
            <Slider
                value={attackTime}
                min={0.001}
                max={2}
                step={0.01}
                label="Attack"
                unit="s"
                orientation="vertical"
                color="var(--pastel-pink)"
                onChange={(v) => updateParam('attack', v, setAttackTime)}
            />
            <Slider
                value={decayTime}
                min={0.001}
                max={2}
                step={0.01}
                label="Decay"
                unit="s"
                orientation="vertical"
                color="var(--pastel-lavender)"
                onChange={(v) => updateParam('decay', v, setDecayTime)}
            />
            <Slider
                value={sustainLevel}
                min={0}
                max={1}
                step={0.01}
                label="Sustain"
                unit=""
                orientation="vertical"
                color="var(--pastel-mint)"
                onChange={(v) => updateParam('sustain', v, setSustainLevel)}
            />
            <Slider
                value={releaseTime}
                min={0.01}
                max={3}
                step={0.01}
                label="Release"
                unit="s"
                orientation="vertical"
                color="var(--pastel-peach)"
                onChange={(v) => updateParam('release', v, setReleaseTime)}
            />
        </div>
    );
}
