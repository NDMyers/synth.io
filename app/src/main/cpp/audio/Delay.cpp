#include "Delay.h"
#include <algorithm>
#include <cmath>

namespace synthio {

Delay::Delay() {
    // Max delay of 1 second at 48kHz
    mMaxDelaySamples = 48000;
    mBufferL.resize(mMaxDelaySamples, 0.0f);
    mBufferR.resize(mMaxDelaySamples, 0.0f);
    updateDelaySamples();
}

void Delay::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    mMaxDelaySamples = static_cast<int>(sampleRate);
    mBufferL.resize(mMaxDelaySamples, 0.0f);
    mBufferR.resize(mMaxDelaySamples, 0.0f);
    updateDelaySamples();
    
    // Update filter coefficient for ~3kHz cutoff
    float cutoff = 3000.0f;
    mFilterCoeff = 1.0f - std::exp(-2.0f * 3.14159f * cutoff / mSampleRate);
}

void Delay::setTime(float timeSeconds) {
    mTime = std::max(0.05f, std::min(0.5f, timeSeconds));
    updateDelaySamples();
}

void Delay::setFeedback(float feedback) {
    mFeedback = std::max(0.0f, std::min(0.8f, feedback));
}

void Delay::setMix(float mix) {
    mMix = std::max(0.0f, std::min(1.0f, mix));
}

void Delay::updateDelaySamples() {
    mDelaySamples = static_cast<int>(mTime * mSampleRate);
    mDelaySamples = std::min(mDelaySamples, mMaxDelaySamples - 1);
}

void Delay::process(float& left, float& right) {
    // Read from delay buffer
    int readPos = mWritePos - mDelaySamples;
    if (readPos < 0) readPos += mMaxDelaySamples;
    
    float delayedL = mBufferL[readPos];
    float delayedR = mBufferR[readPos];
    
    // Low-pass filter the delayed signal for warmth
    mFilterStateL += mFilterCoeff * (delayedL - mFilterStateL);
    mFilterStateR += mFilterCoeff * (delayedR - mFilterStateR);
    
    float filteredL = mFilterStateL;
    float filteredR = mFilterStateR;
    
    // Write to delay buffer (input + filtered feedback)
    mBufferL[mWritePos] = left + filteredL * mFeedback;
    mBufferR[mWritePos] = right + filteredR * mFeedback;
    
    // Increment write position
    mWritePos++;
    if (mWritePos >= mMaxDelaySamples) mWritePos = 0;
    
    // Mix dry and wet signals
    left = left * (1.0f - mMix) + delayedL * mMix;
    right = right * (1.0f - mMix) + delayedR * mMix;
}

} // namespace synthio
