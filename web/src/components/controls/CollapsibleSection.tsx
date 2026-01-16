'use client';

/**
 * Collapsible Section Component
 * Accordion-style section for grouping controls
 * Styled to match Android CollapsibleSection with tinted header
 */

import { useState } from 'react';

interface CollapsibleSectionProps {
    title: string;
    defaultOpen?: boolean;
    accentColor?: string;
    children: React.ReactNode;
}

export default function CollapsibleSection({
    title,
    defaultOpen = true,
    accentColor = 'var(--pastel-pink)',
    children,
}: CollapsibleSectionProps) {
    const [isOpen, setIsOpen] = useState(defaultOpen);

    return (
        <div
            className="rounded-xl overflow-hidden shadow-md"
            style={{
                background: 'var(--surface)',
            }}
        >
            {/* Header - Tinted background per mobile spec */}
            <button
                onClick={(e) => { e.stopPropagation(); setIsOpen(!isOpen); }}
                className="w-full flex items-center justify-between px-4 py-3 transition-colors"
                style={{
                    background: isOpen
                        ? `color-mix(in srgb, ${accentColor} 15%, var(--surface))`
                        : 'var(--surface)',
                }}
            >
                <div className="flex items-center gap-3">
                    {/* Accent indicator */}
                    <div
                        className="w-1.5 h-5 rounded-full"
                        style={{ backgroundColor: accentColor }}
                    />
                    <span className="text-sm font-semibold text-[var(--foreground)]">
                        {title}
                    </span>
                </div>

                {/* Chevron */}
                <svg
                    className={`w-5 h-5 text-[var(--text-secondary)] transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
            </button>

            {/* Content with smooth animation */}
            <div
                className={`
          grid transition-all duration-300 ease-in-out
          ${isOpen ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}
        `}
            >
                <div className="overflow-hidden">
                    <div className="px-4 pb-4 pt-2">
                        {children}
                    </div>
                </div>
            </div>
        </div>
    );
}
