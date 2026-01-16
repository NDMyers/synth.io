'use client';

/**
 * Filter Control Panel
 * Cutoff and Resonance knobs
 */

import { useSynthStore } from '@/stores/synth-store';
import Knob from './Knob';

interface FilterPanelProps {
    synthNode: AudioWorkletNode | null;
}

export default function FilterPanel({ synthNode }: FilterPanelProps) {
    const {
        filterCutoff,
        setFilterCutoff,
        filterResonance,
        setFilterResonance,
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
                value={filterCutoff}
                min={100}
                max={20000}
                step={10}
                label="Cutoff"
                unit="Hz"
                color="var(--pastel-blue)"
                onChange={(v) => updateParam('filterCutoff', v, setFilterCutoff)}
            />
            <Knob
                value={filterResonance}
                min={0.1}
                max={20}
                step={0.1}
                label="Resonance"
                unit=""
                color="var(--pastel-coral)"
                onChange={(v) => updateParam('filterResonance', v, setFilterResonance)}
            />
        </div>
    );
}
