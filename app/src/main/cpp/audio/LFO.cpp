#include "LFO.h"
#include <algorithm>

namespace synthio {

LFO::LFO() {
    updatePhaseIncrement();
}

void LFO::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    updatePhaseIncrement();
}

void LFO::setRate(float rateHz) {
    mRate = std::max(0.1f, std::min(20.0f, rateHz));
    updatePhaseIncrement();
}

void LFO::setPitchDepth(float depth) {
    mPitchDepth = std::max(0.0f, std::min(1.0f, depth));
}

void LFO::setFilterDepth(float depth) {
    mFilterDepth = std::max(0.0f, std::min(1.0f, depth));
}

void LFO::setPWMDepth(float depth) {
    mPWMDepth = std::max(0.0f, std::min(1.0f, depth));
}

void LFO::updatePhaseIncrement() {
    mPhaseIncrement = mRate / mSampleRate;
}

void LFO::reset() {
    mPhase = 0.0f;
    mCurrentValue = 0.0f;
}

void LFO::tick() {
    mCurrentValue = generateTriangle();
    
    mPhase += mPhaseIncrement;
    if (mPhase >= 1.0f) {
        mPhase -= 1.0f;
    }
}

float LFO::generateTriangle() {
    // Triangle wave: -1 to 1
    if (mPhase < 0.5f) {
        return 4.0f * mPhase - 1.0f;
    } else {
        return 3.0f - 4.0f * mPhase;
    }
}

float LFO::getPitchMod() const {
    // Returns pitch deviation in semitones (max ±2 semitones)
    return mCurrentValue * mPitchDepth * 2.0f;
}

float LFO::getFilterMod() const {
    // Returns -1.0 to 1.0 scaled by depth
    return mCurrentValue * mFilterDepth;
}

float LFO::getPWMMod() const {
    // Returns pulse width modulation (max ±0.4 to keep PW in safe range)
    return mCurrentValue * mPWMDepth * 0.4f;
}

} // namespace synthio
