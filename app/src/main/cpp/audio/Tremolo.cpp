#define _USE_MATH_DEFINES
#include "Tremolo.h"
#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

Tremolo::Tremolo() {
    updatePhaseIncrement();
}

void Tremolo::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    // Update smoothing coefficient for new sample rate
    // LDR has inherent lag, ~5-10ms response time
    float smoothingTimeMs = 8.0f;
    mSmoothingCoeff = std::exp(-1.0f / (smoothingTimeMs * 0.001f * mSampleRate));
    updatePhaseIncrement();
}

void Tremolo::setRate(float rateHz) {
    mRate = std::max(0.5f, std::min(10.0f, rateHz));
    updatePhaseIncrement();
}

void Tremolo::setDepth(float depth) {
    mDepth = std::max(0.0f, std::min(1.0f, depth));
}

void Tremolo::updatePhaseIncrement() {
    mPhaseIncrement = mRate / mSampleRate;
}

float Tremolo::process(float input) {
    // Skip processing if depth is 0
    if (mDepth < 0.001f) {
        return input;
    }
    
    // Generate LFO with sine wave
    // LDR response is slightly asymmetric - faster attack than release
    float lfoValue = std::sin(mPhase * 2.0f * static_cast<float>(M_PI));
    
    // Map sine (-1 to 1) to modulation amount
    // Wurlitzer 200A tremolo ranges from subtle wobble to deep pulsing
    // At full depth, amplitude can drop to ~30% of original
    float modRange = mDepth * 0.70f;  // Max 70% modulation depth
    float targetMod = 1.0f - modRange * 0.5f * (1.0f - lfoValue);
    
    // Smooth the modulation (LDR lag characteristic)
    mCurrentMod = mCurrentMod * mSmoothingCoeff + targetMod * (1.0f - mSmoothingCoeff);
    
    // Advance phase
    mPhase += mPhaseIncrement;
    if (mPhase >= 1.0f) mPhase -= 1.0f;
    
    return input * mCurrentMod;
}

void Tremolo::process(float& left, float& right) {
    // Skip processing if depth is 0
    if (mDepth < 0.001f) {
        return;
    }
    
    // Generate LFO
    float lfoValue = std::sin(mPhase * 2.0f * static_cast<float>(M_PI));
    
    // Wurlitzer 200A tremolo with increased range
    float modRange = mDepth * 0.70f;
    float targetMod = 1.0f - modRange * 0.5f * (1.0f - lfoValue);
    mCurrentMod = mCurrentMod * mSmoothingCoeff + targetMod * (1.0f - mSmoothingCoeff);
    
    // Apply to both channels
    left *= mCurrentMod;
    right *= mCurrentMod;
    
    // Advance phase
    mPhase += mPhaseIncrement;
    if (mPhase >= 1.0f) mPhase -= 1.0f;
}

} // namespace synthio
