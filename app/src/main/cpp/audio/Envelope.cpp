#include "Envelope.h"
#include <cmath>
#include <algorithm>

namespace synthio {

Envelope::Envelope() {
    calculateRates();
}

void Envelope::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    calculateRates();
}

void Envelope::setAttack(float attackTime) {
    mAttackTime = std::max(0.001f, attackTime);
    mAttackRate = calculateRate(mAttackTime);
}

void Envelope::setDecay(float decayTime) {
    mDecayTime = std::max(0.001f, decayTime);
    mDecayRate = calculateRate(mDecayTime);
}

void Envelope::setSustain(float sustainLevel) {
    mSustainLevel = std::max(0.0f, std::min(1.0f, sustainLevel));
}

void Envelope::setRelease(float releaseTime) {
    mReleaseTime = std::max(0.001f, releaseTime);
    mReleaseRate = calculateRate(mReleaseTime);
}

void Envelope::gate(bool isOn) {
    if (isOn) {
        mStage = EnvelopeStage::ATTACK;
        // Don't reset mCurrentLevel to allow retriggering from current level
    } else if (mStage != EnvelopeStage::IDLE) {
        mStage = EnvelopeStage::RELEASE;
    }
}

void Envelope::calculateRates() {
    mAttackRate = calculateRate(mAttackTime);
    mDecayRate = calculateRate(mDecayTime);
    mReleaseRate = calculateRate(mReleaseTime);
}

float Envelope::calculateRate(float time) {
    // Calculate increment per sample to reach target in given time
    // For linear ramp: rate = 1.0 / (time * sampleRate)
    // For exponential-ish feel, we use a slightly faster rate
    return 1.0f / (time * mSampleRate);
}

float Envelope::nextSample() {
    switch (mStage) {
        case EnvelopeStage::IDLE:
            mCurrentLevel = 0.0f;
            break;
            
        case EnvelopeStage::ATTACK:
            // Linear rise to 1.0 (sounds good for attack)
            mCurrentLevel += mAttackRate;
            if (mCurrentLevel >= 1.0f) {
                mCurrentLevel = 1.0f;
                mStage = EnvelopeStage::DECAY;
            }
            break;
            
        case EnvelopeStage::DECAY:
            // Exponential-ish fall to sustain level
            mCurrentLevel -= mDecayRate * (mCurrentLevel - mSustainLevel + 0.001f);
            if (mCurrentLevel <= mSustainLevel + 0.0001f) {
                mCurrentLevel = mSustainLevel;
                mStage = EnvelopeStage::SUSTAIN;
            }
            break;
            
        case EnvelopeStage::SUSTAIN:
            // Hold at sustain level
            mCurrentLevel = mSustainLevel;
            break;
            
        case EnvelopeStage::RELEASE:
            // Linear fall to 0 (simple and reliable)
            mCurrentLevel -= mReleaseRate;
            if (mCurrentLevel <= 0.0f) {
                mCurrentLevel = 0.0f;
                mStage = EnvelopeStage::IDLE;
            }
            break;
    }
    
    // Clamp to valid range
    mCurrentLevel = std::max(0.0f, std::min(1.0f, mCurrentLevel));
    
    return mCurrentLevel;
}

} // namespace synthio
