#define _USE_MATH_DEFINES
#include "WurlitzerVoice.h"
#include <cmath>
#include <algorithm>
#include <random>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

// ========== Envelope Implementation ==========

void WurlitzerVoice::Envelope::trigger(float attack, float decay, float sustain, float release, float sr) {
    level = 0.0f;
    sustainLevel = sustain;
    
    // Convert times to rates (per-sample increments)
    attackRate = attack > 0.0005f ? 1.0f / (attack * sr) : 1.0f;
    decayRate = decay > 0.001f ? 1.0f / (decay * sr) : 0.01f;
    releaseRate = release > 0.001f ? 1.0f / (release * sr) : 0.01f;
    
    attacking = true;
    decaying = false;
    sustaining = false;
    releasing = false;
}

void WurlitzerVoice::Envelope::release() {
    if (attacking || decaying || sustaining) {
        releasing = true;
        attacking = false;
        decaying = false;
        sustaining = false;
    }
}

float WurlitzerVoice::Envelope::process() {
    if (attacking) {
        level += attackRate;
        if (level >= 1.0f) {
            level = 1.0f;
            attacking = false;
            decaying = true;
        }
    } else if (decaying) {
        // Exponential decay for natural piano-like feel
        level -= decayRate * (level - sustainLevel + 0.001f);
        if (level <= sustainLevel + 0.001f) {
            level = sustainLevel;
            decaying = false;
            sustaining = true;
        }
    } else if (sustaining) {
        level = sustainLevel;
    } else if (releasing) {
        // Exponential release
        level -= releaseRate * (level + 0.001f);
        if (level <= 0.0005f) {
            level = 0.0f;
            releasing = false;
        }
    }
    
    return level;
}

bool WurlitzerVoice::Envelope::isActive() const {
    return attacking || decaying || sustaining || releasing || level > 0.0005f;
}

// ========== WurlitzerVoice Implementation ==========

WurlitzerVoice::WurlitzerVoice() {
}

void WurlitzerVoice::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
}

void WurlitzerVoice::noteOn(int midiNote, float frequency, float velocity) {
    mMidiNote = midiNote;
    mFrequency = frequency;
    mVelocity = std::max(0.0f, std::min(1.0f, velocity));
    mNoteOn = true;
    mActive = true;
    
    // Reset oscillator phases
    mPhase1 = 0.0f;
    mPhase2 = 0.0f;
    mPhase3 = 0.0f;
    mPhase4 = 0.0f;
    mPhaseNoise = 0.0f;
    
    // Reset state
    mFeedback = 0.0f;
    mLastSample = 0.0f;
    mDCBlocker = 0.0f;
    
    setupEnvelopes(velocity);
}

void WurlitzerVoice::noteOff() {
    mNoteOn = false;
    mAmpEnv.release();
    mBarkEnv.release();
    mHarmonicEnv.release();
    mTineEnv.release();
}

void WurlitzerVoice::setupEnvelopes(float velocity) {
    // Smooth, warm, buttery Wurlitzer character
    // Gentle attack, rich sustain, relaxing tone
    
    float velCurve = velocity * 0.7f + 0.3f;  // Compress velocity range for smoother response
    
    // ===== MAIN AMPLITUDE ENVELOPE =====
    // Soft attack for smooth onset, long natural decay
    float attackTime = 0.008f + (1.0f - velocity) * 0.012f;  // 8-20ms (slower, smoother)
    float decayTime = 2.0f + (1.0f - velocity) * 1.5f;       // 2.0-3.5s (longer, more sustain)
    mAmpEnv.trigger(attackTime, decayTime, 0.0f, 0.35f, mSampleRate);
    
    // ===== BARK ENVELOPE =====
    // Subtle, gentle attack coloration - not aggressive
    float barkDecay = 0.04f + (1.0f - velocity) * 0.03f;  // 40-70ms (slower, gentler)
    mBarkEnv.trigger(0.003f, barkDecay, 0.0f, 0.02f, mSampleRate);
    mBarkIntensity = 0.08f + velocity * 0.12f;  // Much more subtle (0.08-0.20)
    
    // ===== HARMONIC ENVELOPE =====
    // Harmonics blend smoothly, decay gracefully
    float harmonicDecay = 0.5f + (1.0f - velocity) * 0.3f;  // 500-800ms
    mHarmonicEnv.trigger(0.005f, harmonicDecay, 0.12f, 0.25f, mSampleRate);
    
    // ===== TINE RESONANCE ENVELOPE =====
    // Gentle bell-like sustain
    float tineDecay = 1.2f + velocity * 0.5f;  // 1.2-1.7s (longer ring)
    mTineEnv.trigger(0.008f, tineDecay, 0.15f, 0.3f, mSampleRate);
    
    // ===== VELOCITY-BASED HARMONIC LEVELS =====
    // Emphasis on warm fundamental, gentle harmonics
    mFundamentalLevel = 0.65f + (1.0f - velocity) * 0.10f;  // 0.65-0.75 (more fundamental)
    mSecondHarmonicLevel = 0.12f + velocity * 0.10f;         // 0.12-0.22 (gentler)
    mThirdHarmonicLevel = 0.05f + velocity * 0.08f;          // 0.05-0.13 (much softer)
}

float WurlitzerVoice::sine(float phase) {
    return std::sin(phase * 2.0f * static_cast<float>(M_PI));
}

float WurlitzerVoice::softClip(float x) {
    // Gentle saturation like the Wurlitzer's internal amp
    if (x > 1.0f) return 1.0f - 1.0f / (x + 1.0f);
    if (x < -1.0f) return -1.0f + 1.0f / (-x + 1.0f);
    return x - (x * x * x) / 6.0f;  // Subtle odd-harmonic distortion
}

float WurlitzerVoice::generateHammerNoise() {
    // Simple noise for hammer impact transient
    static std::mt19937 rng(42);
    static std::uniform_real_distribution<float> dist(-1.0f, 1.0f);
    return dist(rng);
}

float WurlitzerVoice::nextSample() {
    if (!mActive) return 0.0f;
    
    // Process all envelopes
    float ampEnv = mAmpEnv.process();
    float barkEnv = mBarkEnv.process();
    float harmonicEnv = mHarmonicEnv.process();
    float tineEnv = mTineEnv.process();
    
    // Check if voice should deactivate
    if (!mAmpEnv.isActive()) {
        mActive = false;
        return 0.0f;
    }
    
    // Calculate phase increments
    float phaseInc = mFrequency / mSampleRate;
    
    // ===== GENTLE ATTACK COLORATION =====
    // Subtle FM modulation for warmth, not aggressive "zing"
    float barkMod = sine(mPhase4) * mBarkIntensity * barkEnv * 0.8f;  // Reduced modulation
    
    // Very subtle hammer transient - almost imperceptible
    float hammerNoise = generateHammerNoise() * barkEnv * barkEnv * 0.03f * mBarkIntensity;
    
    // ===== MAIN OSCILLATORS WITH FM MODULATION =====
    // Fundamental with bark modulation and slight feedback
    float fundamental = sine(mPhase1 + barkMod + mFeedback * FEEDBACK_AMOUNT);
    fundamental *= mFundamentalLevel * ampEnv;
    
    // 2nd harmonic (octave) - decays with harmonic envelope
    // Slight inharmonicity from real reed behavior
    float second = sine(mPhase2 * TINE_INHARMONICITY);
    second *= mSecondHarmonicLevel * harmonicEnv * ampEnv;
    
    // 3rd harmonic - crucial for Wurlitzer's "reedy" character
    // This is what distinguishes it from Rhodes
    float third = sine(mPhase3 * TINE_INHARMONICITY * TINE_INHARMONICITY);
    third *= mThirdHarmonicLevel * harmonicEnv * ampEnv;
    
    // ===== TINE RESONANCE =====
    // Gentle bell-like quality from metal reed vibration
    // Soft, warm overtones
    float tineResonance = sine(mPhase1 * 4.997f) * 0.025f;  // ~5th partial (softer)
    tineResonance += sine(mPhase1 * 5.994f) * 0.015f;       // ~6th partial (softer)
    tineResonance *= tineEnv * ampEnv * (0.6f + mVelocity * 0.3f);
    
    // ===== MIX ALL COMPONENTS =====
    float sample = fundamental + second + third + tineResonance + hammerNoise;
    
    // ===== WARM AMP COLORATION =====
    // Gentle saturation for warmth without harshness
    sample = softClip(sample * 1.1f) * 0.85f;
    
    // ===== DC BLOCKING =====
    // Remove any DC offset that might accumulate
    float dcBlocked = sample - mDCBlocker;
    mDCBlocker = mDCBlocker * 0.999f + sample * 0.001f;
    sample = dcBlocked;
    
    // Update feedback for next sample
    mFeedback = sample;
    mLastSample = sample;
    
    // ===== ADVANCE PHASES =====
    mPhase1 += phaseInc;
    mPhase2 += phaseInc * 2.0f;   // Octave
    mPhase3 += phaseInc * 3.0f;   // 3rd harmonic
    mPhase4 += phaseInc * 6.0f;   // FM modulator (higher frequency for bark)
    
    // Wrap phases to prevent overflow
    if (mPhase1 >= 1.0f) mPhase1 -= 1.0f;
    if (mPhase2 >= 1.0f) mPhase2 -= 1.0f;
    if (mPhase3 >= 1.0f) mPhase3 -= 1.0f;
    if (mPhase4 >= 1.0f) mPhase4 -= 1.0f;
    
    return sample;
}

bool WurlitzerVoice::isActive() const {
    return mActive;
}

} // namespace synthio
