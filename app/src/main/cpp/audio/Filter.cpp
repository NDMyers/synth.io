#define _USE_MATH_DEFINES
#include "Filter.h"
#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

constexpr float PI = M_PI;

Filter::Filter() {
    calculateLPFCoefficients();
    calculateHPFCoefficient();
}

void Filter::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    calculateLPFCoefficients();
    calculateHPFCoefficient();
}

void Filter::setCutoff(float cutoffHz) {
    mTargetCutoff = std::max(20.0f, std::min(20000.0f, cutoffHz));
}

void Filter::setResonance(float resonance) {
    // Allow full range 0-1 where 1.0 enables self-oscillation
    mResonance = std::max(0.0f, std::min(1.0f, resonance));
    calculateLPFCoefficients();
}

void Filter::setHPFCutoff(float cutoffHz) {
    mHPFCutoff = std::max(0.0f, std::min(1000.0f, cutoffHz));
    calculateHPFCoefficient();
}

void Filter::setKeyTracking(float amount) {
    mKeyTracking = std::max(0.0f, std::min(1.0f, amount));
}

void Filter::setNoteFrequency(float freq) {
    mNoteFrequency = freq;
}

void Filter::reset() {
    mX1 = mX2 = mY1 = mY2 = 0.0f;
    mHPFState = 0.0f;
}

float Filter::process(float input) {
    // Apply key tracking to cutoff
    float keyTrackOffset = 0.0f;
    if (mKeyTracking > 0.0f) {
        // Track relative to middle C (261.63 Hz)
        float octaveOffset = std::log2(mNoteFrequency / 261.63f);
        // Each octave adds/subtracts from cutoff (scaled by tracking amount)
        keyTrackOffset = octaveOffset * 2000.0f * mKeyTracking;
    }
    
    float effectiveCutoff = mTargetCutoff + keyTrackOffset;
    effectiveCutoff = std::max(20.0f, std::min(20000.0f, effectiveCutoff));
    
    // Smooth cutoff changes
    if (std::abs(mCutoff - effectiveCutoff) > 1.0f) {
        mCutoff += (effectiveCutoff - mCutoff) * mSmoothingFactor;
        calculateLPFCoefficients();
    }
    
    // Low-pass filter (Biquad Direct Form I)
    float lpfOutput = mA0 * input + mA1 * mX1 + mA2 * mX2 - mB1 * mY1 - mB2 * mY2;
    
    // Soft saturation to prevent filter runaway at high resonance
    // Using tanh-based saturation that's transparent at normal levels
    lpfOutput = softSaturate(lpfOutput);
    
    // Update LPF delay line
    mX2 = mX1;
    mX1 = input;
    mY2 = mY1;
    mY1 = lpfOutput;
    
    // Apply resonance gain compensation
    // High resonance boosts signal, so we reduce output proportionally
    float gainCompensation = 1.0f / (1.0f + mResonance * 2.0f);
    lpfOutput *= gainCompensation;
    
    // High-pass filter (one-pole)
    float output;
    if (mHPFCutoff < 1.0f) {
        // Bass boost mode when HPF is at 0
        // Subtle low-frequency enhancement
        output = lpfOutput * mBassBoostAmount;
    } else {
        // Apply HPF
        float hpfInput = lpfOutput;
        mHPFState = mHPFState + mHPFCoeff * (hpfInput - mHPFState);
        output = hpfInput - mHPFState;
    }
    
    return output;
}

float Filter::softSaturate(float x) {
    // Soft saturation that's transparent below threshold
    // and gently compresses above it to prevent clipping
    const float threshold = 0.8f;
    float absX = std::abs(x);
    
    if (absX <= threshold) {
        return x;
    }
    
    // Soft knee saturation above threshold
    float sign = (x > 0) ? 1.0f : -1.0f;
    float excess = absX - threshold;
    float compressed = threshold + (1.0f - threshold) * std::tanh(excess * 3.0f);
    return sign * compressed;
}

void Filter::calculateLPFCoefficients() {
    // Map resonance 0-1 to Q
    // At resonance = 1.0, Q goes high enough for self-oscillation
    float Q;
    if (mResonance < 0.95f) {
        // Normal range: Q from 0.707 to ~15
        Q = 0.707f + mResonance * 15.0f;
    } else {
        // Self-oscillation range: Q from 15 to 50+
        float t = (mResonance - 0.95f) / 0.05f;  // 0 to 1 in last 5%
        Q = 15.0f + t * 35.0f;  // Ramps up to Q=50
    }
    
    // Clamp cutoff to valid range
    float fc = std::min(mCutoff, mSampleRate * 0.499f);
    
    // Angular frequency
    float omega = 2.0f * PI * fc / mSampleRate;
    float sinOmega = std::sin(omega);
    float cosOmega = std::cos(omega);
    float alpha = sinOmega / (2.0f * Q);
    
    // Low-pass filter coefficients
    float b0 = (1.0f - cosOmega) / 2.0f;
    float b1 = 1.0f - cosOmega;
    float b2 = (1.0f - cosOmega) / 2.0f;
    float a0 = 1.0f + alpha;
    float a1 = -2.0f * cosOmega;
    float a2 = 1.0f - alpha;
    
    // Normalize coefficients
    mA0 = b0 / a0;
    mA1 = b1 / a0;
    mA2 = b2 / a0;
    mB1 = a1 / a0;
    mB2 = a2 / a0;
}

void Filter::calculateHPFCoefficient() {
    if (mHPFCutoff < 1.0f) {
        mHPFCoeff = 0.0f;
    } else {
        // Simple one-pole HPF coefficient
        float fc = std::min(mHPFCutoff, mSampleRate * 0.499f);
        mHPFCoeff = 1.0f - std::exp(-2.0f * PI * fc / mSampleRate);
    }
}

} // namespace synthio
