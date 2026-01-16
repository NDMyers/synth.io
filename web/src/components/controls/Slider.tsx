'use client';

/**
 * Slider Control Component
 * Vertical or horizontal slider for synth parameters
 * Fixed vertical mode with proper touch/pointer handling
 */

import { useCallback, useRef, useState } from 'react';

interface SliderProps {
    value: number;
    min: number;
    max: number;
    step?: number;
    label: string;
    unit?: string;
    onChange: (value: number) => void;
    orientation?: 'horizontal' | 'vertical';
    color?: string;
}

export default function Slider({
    value,
    min,
    max,
    step = 0.01,
    label,
    unit = '',
    onChange,
    orientation = 'horizontal',
    color = 'var(--pastel-pink)',
}: SliderProps) {
    const isVertical = orientation === 'vertical';
    const trackRef = useRef<HTMLDivElement>(null);
    const [isDragging, setIsDragging] = useState(false);

    // Normalize value for display (0-100%)
    const normalizedValue = ((value - min) / (max - min)) * 100;

    // Format display value
    const displayValue = value < 1 ? value.toFixed(2) : value < 100 ? value.toFixed(1) : Math.round(value);

    // Calculate value from pointer position
    const getValueFromPosition = useCallback((clientX: number, clientY: number) => {
        if (!trackRef.current) return value;

        const rect = trackRef.current.getBoundingClientRect();
        let percentage: number;

        if (isVertical) {
            // For vertical, 0% is at bottom, 100% at top
            percentage = 1 - (clientY - rect.top) / rect.height;
        } else {
            // For horizontal, 0% is at left, 100% at right
            percentage = (clientX - rect.left) / rect.width;
        }

        // Clamp percentage
        percentage = Math.max(0, Math.min(1, percentage));

        // Convert to value and snap to step
        let newValue = min + percentage * (max - min);
        newValue = Math.round(newValue / step) * step;
        newValue = Math.max(min, Math.min(max, newValue));

        return newValue;
    }, [isVertical, min, max, step, value]);

    const handlePointerDown = useCallback((e: React.PointerEvent) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragging(true);
        (e.target as HTMLElement).setPointerCapture(e.pointerId);

        const newValue = getValueFromPosition(e.clientX, e.clientY);
        onChange(newValue);
    }, [getValueFromPosition, onChange]);

    const handlePointerMove = useCallback((e: React.PointerEvent) => {
        if (!isDragging) return;
        e.preventDefault();

        const newValue = getValueFromPosition(e.clientX, e.clientY);
        onChange(newValue);
    }, [isDragging, getValueFromPosition, onChange]);

    const handlePointerUp = useCallback(() => {
        setIsDragging(false);
    }, []);

    return (
        <div className={`flex ${isVertical ? 'flex-col items-center' : 'flex-col w-full'} gap-2`}>
            <span className="text-xs text-[var(--text-secondary)] font-medium whitespace-nowrap">
                {label}
            </span>

            <div
                ref={trackRef}
                className={`
          relative cursor-pointer select-none
          ${isVertical ? 'h-24 w-6' : 'w-full h-6'}
        `}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerLeave={handlePointerUp}
                style={{ touchAction: 'none' }}
            >
                {/* Track background */}
                <div
                    className={`
            absolute bg-[var(--background-light)] rounded-full
            ${isVertical ? 'w-2 h-full left-1/2 -translate-x-1/2' : 'h-2 w-full top-1/2 -translate-y-1/2'}
          `}
                />

                {/* Filled track */}
                <div
                    className="absolute rounded-full transition-all duration-75"
                    style={{
                        backgroundColor: color,
                        ...(isVertical
                            ? { width: '8px', left: '50%', transform: 'translateX(-50%)', bottom: 0, height: `${normalizedValue}%` }
                            : { height: '8px', top: '50%', transform: 'translateY(-50%)', left: 0, width: `${normalizedValue}%` }
                        ),
                    }}
                />

                {/* Thumb */}
                <div
                    className={`
            absolute w-5 h-5 rounded-full shadow-md pointer-events-none
            transition-transform duration-75
            ${isDragging ? 'scale-110' : ''}
          `}
                    style={{
                        backgroundColor: color,
                        border: '2px solid white',
                        ...(isVertical
                            ? { left: '50%', transform: 'translateX(-50%)', bottom: `calc(${normalizedValue}% - 10px)` }
                            : { top: '50%', transform: 'translateY(-50%)', left: `calc(${normalizedValue}% - 10px)` }
                        ),
                    }}
                />
            </div>

            <span className="text-xs text-[var(--foreground)] font-mono">
                {displayValue}{unit}
            </span>
        </div>
    );
}
