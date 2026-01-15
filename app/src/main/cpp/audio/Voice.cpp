#include "Voice.h"
#include <cmath>
#include <algorithm>

namespace synthio {

Voice::Voice() : mRng(std::random_device{}()) {
    // Default envelope settings for a nice synth sound
    mAmpEnvelope.setAttack(0.01f);
    mAmpEnvelope.setDecay(0.2f);
    mAmpEnvelope.setSustain(0.7f);
    mAmpEnvelope.setRelease(0.3f);
    
    // Filter envelope (faster for percussive sound)
    mFilterEnvelope.setAttack(0.005f);
    mFilterEnvelope.setDecay(0.3f);
    mFilterEnvelope.setSustain(0.3f);
    mFilterEnvelope.setRelease(0.2f);
    
    // Sub-oscillator is always square wave
    mSubOscillator.setWaveform(Waveform::SQUARE);
}

void Voice::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    mOscillator.setSampleRate(sampleRate);
    mSubOscillator.setSampleRate(sampleRate);
    mFilter.setSampleRate(sampleRate);
    mAmpEnvelope.setSampleRate(sampleRate);
    mFilterEnvelope.setSampleRate(sampleRate);
    updateGlideCoefficient();
}

void Voice::noteOn(int midiNote, float frequency) {
    mMidiNote = midiNote;
    mTargetFrequency = frequency;
    
    // Handle glide
    if (mGlideEnabled && !mFirstNote && mGlideTime > 0.0f) {
        // Glide from current frequency to target
        // mCurrentFrequency keeps its value for smooth transition
    } else {
        // No glide - jump to target
        mCurrentFrequency = frequency;
    }
    mFirstNote = false;
    
    // Set oscillator frequencies (with detune)
    float detuned = mCurrentFrequency * mDetuneRatio;
    mOscillator.setFrequency(detuned);
    mSubOscillator.setFrequency(detuned * 0.5f);  // One octave below
    
    // Set filter key tracking
    mFilter.setNoteFrequency(frequency);
    
    mOscillator.reset();
    mSubOscillator.reset();
    mFilter.reset();
    mAmpEnvelope.gate(true);
    mFilterEnvelope.gate(true);
    mState = VoiceState::ACTIVE;
}

void Voice::noteOff() {
    mAmpEnvelope.gate(false);
    mFilterEnvelope.gate(false);
    mState = VoiceState::RELEASING;
}

void Voice::setWaveform(Waveform waveform) {
    mOscillator.setWaveform(waveform);
}

void Voice::setPulseWidth(float width) {
    mBasePulseWidth = std::max(0.1f, std::min(0.9f, width));
}

void Voice::setSubOscLevel(float level) {
    mSubOscLevel = std::max(0.0f, std::min(1.0f, level));
}

void Voice::setNoiseLevel(float level) {
    mNoiseLevel = std::max(0.0f, std::min(1.0f, level));
}

void Voice::setFilterCutoff(float cutoffHz) {
    mFilterBaseCutoff = cutoffHz;
}

void Voice::setFilterResonance(float resonance) {
    mFilter.setResonance(resonance);
}

void Voice::setFilterEnvelopeAmount(float amount) {
    mFilterEnvAmount = amount;
}

void Voice::setFilterKeyTracking(float amount) {
    mFilter.setKeyTracking(amount);
}

void Voice::setHPFCutoff(float cutoffHz) {
    mFilter.setHPFCutoff(cutoffHz);
}

void Voice::setAttack(float time) {
    mAmpEnvelope.setAttack(time);
}

void Voice::setDecay(float time) {
    mAmpEnvelope.setDecay(time);
}

void Voice::setSustain(float level) {
    mAmpEnvelope.setSustain(level);
}

void Voice::setRelease(float time) {
    mAmpEnvelope.setRelease(time);
}

void Voice::setGlideTime(float time) {
    mGlideTime = std::max(0.0f, std::min(2.0f, time));
    updateGlideCoefficient();
}

void Voice::setGlideEnabled(bool enabled) {
    mGlideEnabled = enabled;
    if (!enabled) {
        mFirstNote = true;  // Reset so next note doesn't glide
    }
}

void Voice::updateGlideCoefficient() {
    if (mGlideTime <= 0.0f) {
        mGlideCoeff = 1.0f;  // Instant
    } else {
        // Exponential glide: freq = current + (target - current) * (1 - e^(-t/tau))
        // Per sample: mGlideCoeff determines how much to move toward target
        float tau = mGlideTime / 5.0f;  // Time constant (reach ~99% in 5*tau)
        mGlideCoeff = 1.0f - std::exp(-1.0f / (tau * mSampleRate));
    }
}

void Voice::applyLFOPitchMod(float semitones) {
    mLFOPitchMod = semitones;
}

void Voice::applyLFOFilterMod(float amount) {
    mLFOFilterMod = amount;
}

void Voice::applyLFOPWMMod(float amount) {
    mLFOPWMMod = amount;
}

void Voice::setDetune(float cents) {
    // Convert cents to frequency ratio
    // 100 cents = 1 semitone, ratio = 2^(cents/1200)
    mDetuneRatio = std::pow(2.0f, cents / 1200.0f);
}

float Voice::generateNoise() {
    return mNoiseDist(mRng);
}

float Voice::nextSample() {
    if (mState == VoiceState::IDLE) {
        return 0.0f;
    }
    
    // Update glide (smooth frequency transition)
    if (mGlideEnabled && mGlideTime > 0.0f) {
        mCurrentFrequency += (mTargetFrequency - mCurrentFrequency) * mGlideCoeff;
    } else {
        mCurrentFrequency = mTargetFrequency;
    }
    
    // Apply LFO pitch modulation
    float pitchModRatio = std::pow(2.0f, mLFOPitchMod / 12.0f);
    float modulatedFreq = mCurrentFrequency * mDetuneRatio * pitchModRatio;
    
    // Update oscillator frequencies
    mOscillator.setFrequency(modulatedFreq);
    mSubOscillator.setFrequency(modulatedFreq * 0.5f);
    
    // Apply LFO PWM modulation
    float modulatedPW = mBasePulseWidth + mLFOPWMMod;
    modulatedPW = std::max(0.1f, std::min(0.9f, modulatedPW));
    mOscillator.setPulseWidth(modulatedPW);
    
    // Generate main oscillator sample
    float mainOsc = mOscillator.nextSample();
    
    // Generate sub-oscillator sample
    float subOsc = mSubOscillator.nextSample() * mSubOscLevel;
    
    // Generate noise
    float noise = generateNoise() * mNoiseLevel;
    
    // Mix oscillators (before filter)
    float sample = mainOsc + subOsc + noise;
    
    // Normalize if multiple sources are active
    float mixLevel = 1.0f + mSubOscLevel * 0.5f + mNoiseLevel * 0.5f;
    sample /= mixLevel;
    
    // Get envelope values
    float ampEnv = mAmpEnvelope.nextSample();
    float filterEnv = mFilterEnvelope.nextSample();
    
    // Modulate filter cutoff with envelope and LFO
    float envMod = filterEnv * mFilterEnvAmount * 10000.0f;
    float lfoMod = mLFOFilterMod * 5000.0f;  // LFO can sweep ±5kHz
    float modulatedCutoff = mFilterBaseCutoff + envMod + lfoMod;
    modulatedCutoff = std::max(20.0f, std::min(20000.0f, modulatedCutoff));
    mFilter.setCutoff(modulatedCutoff);
    
    // Apply filter
    sample = mFilter.process(sample);
    
    // Apply amplitude envelope (VCA)
    sample *= ampEnv;
    
    // Check if voice has finished
    if (!mAmpEnvelope.isActive()) {
        mState = VoiceState::IDLE;
        mMidiNote = -1;
        mFirstNote = true;  // Reset for next note sequence
    }
    
    return sample;
}

} // namespace synthio
