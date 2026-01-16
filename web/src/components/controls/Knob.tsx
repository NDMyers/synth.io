'use client';

/**
 * Knob Control Component
 * Rotary dial for synth parameters with drag interaction
 * Styled to match Android Knob component
 */

import { useCallback, useRef, useState } from 'react';

interface KnobProps {
    value: number;
    min: number;
    max: number;
    step?: number;
    label: string;
    unit?: string;
    onChange: (value: number) => void;
    size?: 'sm' | 'md' | 'lg';
    color?: string;
}

export default function Knob({
    value,
    min,
    max,
    step = 0.01,
    label,
    unit = '',
    onChange,
    size = 'md',
    color = 'var(--pastel-pink)',
}: KnobProps) {
    const knobRef = useRef<HTMLDivElement>(null);
    const [isDragging, setIsDragging] = useState(false);
    const startY = useRef(0);
    const startValue = useRef(value);

    // Convert value to rotation angle (0-270 degrees, starting at -135)
    const normalizedValue = (value - min) / (max - min);
    const rotation = normalizedValue * 270 - 135;

    const handlePointerDown = useCallback((e: React.PointerEvent) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragging(true);
        startY.current = e.clientY;
        startValue.current = value;
        (e.target as HTMLElement).setPointerCapture(e.pointerId);
    }, [value]);

    const handlePointerMove = useCallback((e: React.PointerEvent) => {
        if (!isDragging) return;

        const deltaY = startY.current - e.clientY;
        const sensitivity = (max - min) / 150;
        let newValue = startValue.current + deltaY * sensitivity;

        newValue = Math.max(min, Math.min(max, newValue));
        newValue = Math.round(newValue / step) * step;

        onChange(newValue);
    }, [isDragging, min, max, step, onChange]);

    const handlePointerUp = useCallback(() => {
        setIsDragging(false);
    }, []);

    // Size variations
    const sizes = {
        sm: { outer: 48, inner: 40, indicator: 3, fontSize: 10 },
        md: { outer: 64, inner: 52, indicator: 4, fontSize: 12 },
        lg: { outer: 80, inner: 66, indicator: 5, fontSize: 14 },
    };

    const s = sizes[size];
    const strokeWidth = 4;
    const radius = (s.outer - strokeWidth) / 2;
    const circumference = 2 * Math.PI * radius;
    const arcLength = (270 / 360) * circumference;
    const filledArc = normalizedValue * arcLength;

    // Format display value
    const displayValue = value < 1 ? value.toFixed(2) : value < 100 ? value.toFixed(1) : Math.round(value);

    return (
        <div
            className="flex flex-col items-center gap-1 select-none"
            style={{ touchAction: 'none' }}
        >
            {/* Label */}
            <span className="text-xs text-[var(--text-secondary)] font-medium whitespace-nowrap">
                {label}
            </span>

            {/* Knob container */}
            <div
                ref={knobRef}
                className={`relative cursor-pointer transition-transform duration-100 ${isDragging ? 'scale-110' : ''}`}
                style={{ width: s.outer, height: s.outer }}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerLeave={handlePointerUp}
            >
                {/* SVG Arc Background & Value */}
                <svg
                    width={s.outer}
                    height={s.outer}
                    className="absolute inset-0"
                    style={{ transform: 'rotate(-225deg)' }}
                >
                    {/* Track arc */}
                    <circle
                        cx={s.outer / 2}
                        cy={s.outer / 2}
                        r={radius}
                        fill="none"
                        stroke="var(--background-light)"
                        strokeWidth={strokeWidth}
                        strokeDasharray={`${arcLength} ${circumference}`}
                        strokeLinecap="round"
                    />
                    {/* Filled arc */}
                    <circle
                        cx={s.outer / 2}
                        cy={s.outer / 2}
                        r={radius}
                        fill="none"
                        stroke={color}
                        strokeWidth={strokeWidth}
                        strokeDasharray={`${filledArc} ${circumference}`}
                        strokeLinecap="round"
                        className="transition-all duration-75"
                    />
                </svg>

                {/* Inner knob body */}
                <div
                    className="absolute rounded-full shadow-md border-2 border-[var(--background-light)]"
                    style={{
                        width: s.inner,
                        height: s.inner,
                        top: (s.outer - s.inner) / 2,
                        left: (s.outer - s.inner) / 2,
                        background: `linear-gradient(145deg, var(--surface), color-mix(in srgb, var(--surface) 90%, black))`,
                    }}
                >
                    {/* Indicator line */}
                    <div
                        className="absolute w-full h-full"
                        style={{ transform: `rotate(${rotation}deg)` }}
                    >
                        <div
                            className="absolute left-1/2 -translate-x-1/2 rounded-full"
                            style={{
                                width: s.indicator,
                                height: s.inner / 3,
                                top: 4,
                                backgroundColor: color,
                            }}
                        />
                    </div>

                    {/* Center dot */}
                    <div
                        className="absolute inset-0 m-auto rounded-full"
                        style={{
                            width: 6,
                            height: 6,
                            backgroundColor: color,
                        }}
                    />
                </div>
            </div>

            {/* Value display */}
            <span className="text-xs text-[var(--foreground)] font-mono tabular-nums">
                {displayValue}{unit}
            </span>
        </div>
    );
}
