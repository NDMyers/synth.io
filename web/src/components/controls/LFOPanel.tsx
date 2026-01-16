'use client';

/**
 * LFO Control Panel
 * Rate and Amount knobs for low-frequency oscillator
 */

import { useSynthStore } from '@/stores/synth-store';
import Knob from './Knob';

interface LFOPanelProps {
    synthNode: AudioWorkletNode | null;
}

export default function LFOPanel({ synthNode }: LFOPanelProps) {
    const {
        lfoRate,
        setLfoRate,
        lfoAmount,
        setLfoAmount,
    } = useSynthStore();

    const updateParam = (param: string, value: number, setter: (v: number) => void) => {
        setter(value);
        synthNode?.port.postMessage({
            type: 'setParam',
            data: { param, value },
        });
    };

    return (
        <div className="flex justify-around items-center gap-4 py-2">
            <Knob
                value={lfoRate}
                min={0.1}
                max={20}
                step={0.1}
                label="Rate"
                unit="Hz"
                color="var(--pastel-yellow)"
                onChange={(v) => updateParam('lfoRate', v, setLfoRate)}
            />
            <Knob
                value={lfoAmount}
                min={0}
                max={1}
                step={0.01}
                label="Amount"
                unit=""
                color="var(--pastel-mint)"
                onChange={(v) => updateParam('lfoAmount', v, setLfoAmount)}
            />
        </div>
    );
}
