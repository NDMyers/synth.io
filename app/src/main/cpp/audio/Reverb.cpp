#include "Reverb.h"
#include <algorithm>
#include <cmath>

namespace synthio {

Reverb::Reverb() {
    initializeFilters();
}

void Reverb::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    initializeFilters();
}

void Reverb::initializeFilters() {
    float sampleRateScale = mSampleRate / 48000.0f;
    
    // Initialize comb filters
    for (int i = 0; i < NUM_COMBS; ++i) {
        int delaySize = static_cast<int>(COMB_DELAYS[i] * sampleRateScale) + 1;
        mCombsL[i].buffer.resize(delaySize, 0.0f);
        mCombsL[i].writePos = 0;
        mCombsL[i].filterState = 0.0f;
        
        // Slight offset for stereo width
        int delaySizeR = static_cast<int>((COMB_DELAYS[i] + 23) * sampleRateScale) + 1;
        mCombsR[i].buffer.resize(delaySizeR, 0.0f);
        mCombsR[i].writePos = 0;
        mCombsR[i].filterState = 0.0f;
    }
    
    // Initialize allpass filters  
    for (int i = 0; i < NUM_ALLPASS; ++i) {
        int delaySize = static_cast<int>(ALLPASS_DELAYS[i] * sampleRateScale) + 1;
        mAllpassL[i].buffer.resize(delaySize, 0.0f);
        mAllpassL[i].writePos = 0;
        
        int delaySizeR = static_cast<int>((ALLPASS_DELAYS[i] + 11) * sampleRateScale) + 1;
        mAllpassR[i].buffer.resize(delaySizeR, 0.0f);
        mAllpassR[i].writePos = 0;
    }
}

void Reverb::setSize(float size) {
    mSize = std::max(0.0f, std::min(1.0f, size));
    
    // Update comb filter feedback based on size
    float feedback = 0.5f + mSize * 0.45f;  // 0.5 to 0.95
    for (int i = 0; i < NUM_COMBS; ++i) {
        mCombsL[i].feedback = feedback;
        mCombsR[i].feedback = feedback;
    }
}

void Reverb::setDamping(float damping) {
    mDamping = std::max(0.0f, std::min(1.0f, damping));
    
    for (int i = 0; i < NUM_COMBS; ++i) {
        mCombsL[i].damping = mDamping;
        mCombsR[i].damping = mDamping;
    }
}

void Reverb::setMix(float mix) {
    mMix = std::max(0.0f, std::min(1.0f, mix));
}

void Reverb::reset() {
    for (int i = 0; i < NUM_COMBS; ++i) {
        std::fill(mCombsL[i].buffer.begin(), mCombsL[i].buffer.end(), 0.0f);
        std::fill(mCombsR[i].buffer.begin(), mCombsR[i].buffer.end(), 0.0f);
        mCombsL[i].filterState = 0.0f;
        mCombsR[i].filterState = 0.0f;
    }
    for (int i = 0; i < NUM_ALLPASS; ++i) {
        std::fill(mAllpassL[i].buffer.begin(), mAllpassL[i].buffer.end(), 0.0f);
        std::fill(mAllpassR[i].buffer.begin(), mAllpassR[i].buffer.end(), 0.0f);
    }
}

float Reverb::processComb(CombFilter& comb, float input, int delaySamples) {
    int bufferSize = static_cast<int>(comb.buffer.size());
    delaySamples = std::min(delaySamples, bufferSize - 1);
    
    // Read from buffer
    int readPos = comb.writePos - delaySamples;
    if (readPos < 0) readPos += bufferSize;
    
    float delayed = comb.buffer[readPos];
    
    // Apply damping (low-pass filter in feedback)
    comb.filterState = delayed * (1.0f - comb.damping) + comb.filterState * comb.damping;
    
    // Write to buffer
    comb.buffer[comb.writePos] = input + comb.filterState * comb.feedback;
    
    // Increment write position
    comb.writePos++;
    if (comb.writePos >= bufferSize) comb.writePos = 0;
    
    return delayed;
}

float Reverb::processAllpass(AllpassFilter& ap, float input, int delaySamples) {
    int bufferSize = static_cast<int>(ap.buffer.size());
    delaySamples = std::min(delaySamples, bufferSize - 1);
    
    // Read from buffer
    int readPos = ap.writePos - delaySamples;
    if (readPos < 0) readPos += bufferSize;
    
    float delayed = ap.buffer[readPos];
    float output = -input + delayed;
    
    // Write to buffer
    ap.buffer[ap.writePos] = input + delayed * ap.feedback;
    
    // Increment write position
    ap.writePos++;
    if (ap.writePos >= bufferSize) ap.writePos = 0;
    
    return output;
}

void Reverb::process(float& left, float& right) {
    float sampleRateScale = mSampleRate / 48000.0f;
    
    // Mix input to mono for reverb input
    float monoInput = (left + right) * 0.5f;
    
    // Process parallel comb filters
    float combSumL = 0.0f;
    float combSumR = 0.0f;
    
    for (int i = 0; i < NUM_COMBS; ++i) {
        int delayL = static_cast<int>(COMB_DELAYS[i] * sampleRateScale);
        int delayR = static_cast<int>((COMB_DELAYS[i] + 23) * sampleRateScale);
        
        combSumL += processComb(mCombsL[i], monoInput, delayL);
        combSumR += processComb(mCombsR[i], monoInput, delayR);
    }
    
    // Normalize
    combSumL *= 0.25f;
    combSumR *= 0.25f;
    
    // Process series allpass filters
    float wetL = combSumL;
    float wetR = combSumR;
    
    for (int i = 0; i < NUM_ALLPASS; ++i) {
        int delayL = static_cast<int>(ALLPASS_DELAYS[i] * sampleRateScale);
        int delayR = static_cast<int>((ALLPASS_DELAYS[i] + 11) * sampleRateScale);
        
        wetL = processAllpass(mAllpassL[i], wetL, delayL);
        wetR = processAllpass(mAllpassR[i], wetR, delayR);
    }
    
    // Mix dry and wet
    left = left * (1.0f - mMix) + wetL * mMix;
    right = right * (1.0f - mMix) + wetR * mMix;
}

} // namespace synthio
