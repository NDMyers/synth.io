#include "Chorus.h"
#include <algorithm>
#include <cstring>

namespace synthio {

constexpr float TWO_PI = 2.0f * M_PI;

Chorus::Chorus() {
    // Initialize delay line for max 50ms at 48kHz
    mDelayLineSize = static_cast<int>(0.05f * 48000.0f);
    mDelayLine.resize(mDelayLineSize, 0.0f);
}

void Chorus::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    
    // Resize delay line for new sample rate (max 50ms)
    mDelayLineSize = static_cast<int>(0.05f * mSampleRate);
    mDelayLine.resize(mDelayLineSize, 0.0f);
    reset();
}

void Chorus::setMode(Mode mode) {
    mMode = mode;
    updateModeParams();
}

void Chorus::updateModeParams() {
    switch (mMode) {
        case Mode::MODE_I:
            mCurrentParams = MODE_I_PARAMS;
            break;
        case Mode::MODE_II:
            mCurrentParams = MODE_II_PARAMS;
            break;
        default:
            break;
    }
}

void Chorus::reset() {
    std::fill(mDelayLine.begin(), mDelayLine.end(), 0.0f);
    mWriteIndex = 0;
    mLfoPhase = 0.0f;
}

void Chorus::process(float input, float& outLeft, float& outRight) {
    if (mMode == Mode::OFF) {
        outLeft = input;
        outRight = input;
        return;
    }
    
    // Write input to delay line
    mDelayLine[mWriteIndex] = input;
    
    // Generate sine LFO for smooth modulation
    float lfoValue = std::sin(mLfoPhase * TWO_PI);
    
    // Calculate delay times for left and right (inverted modulation)
    float baseDelaySamples = mCurrentParams.baseDelay * mSampleRate;
    float modDepthSamples = mCurrentParams.depth * mSampleRate;
    
    float delayLeft = baseDelaySamples + lfoValue * modDepthSamples;
    float delayRight = baseDelaySamples - lfoValue * modDepthSamples;  // Inverted
    
    // Clamp delay times
    delayLeft = std::max(1.0f, std::min(delayLeft, static_cast<float>(mDelayLineSize - 1)));
    delayRight = std::max(1.0f, std::min(delayRight, static_cast<float>(mDelayLineSize - 1)));
    
    // Read from delay lines
    float wetLeft = readDelayLine(delayLeft);
    float wetRight = readDelayLine(delayRight);
    
    // Mix dry and wet signals
    float wetMix = mCurrentParams.wetMix;
    float dryMix = 1.0f - wetMix * 0.5f;  // Keep some dry signal
    
    outLeft = input * dryMix + wetLeft * wetMix;
    outRight = input * dryMix + wetRight * wetMix;
    
    // Advance write index
    mWriteIndex = (mWriteIndex + 1) % mDelayLineSize;
    
    // Advance LFO phase
    mLfoPhase += mCurrentParams.rate / mSampleRate;
    if (mLfoPhase >= 1.0f) {
        mLfoPhase -= 1.0f;
    }
}

float Chorus::readDelayLine(float delaySamples) {
    // Calculate read position with linear interpolation
    float readPos = static_cast<float>(mWriteIndex) - delaySamples;
    if (readPos < 0) {
        readPos += static_cast<float>(mDelayLineSize);
    }
    
    int index0 = static_cast<int>(readPos);
    int index1 = (index0 + 1) % mDelayLineSize;
    float frac = readPos - static_cast<float>(index0);
    
    // Linear interpolation
    return mDelayLine[index0] * (1.0f - frac) + mDelayLine[index1] * frac;
}

} // namespace synthio
