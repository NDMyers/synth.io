#include "DrumMachine.h"
#include <algorithm>

namespace synthio {

// Define the static constexpr member
constexpr float DrumMachine::HIHAT_VELOCITIES[16];

DrumMachine::DrumMachine() {
    calculateSamplesPerSixteenth();
}

void DrumMachine::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    mDrumSynth.setSampleRate(sampleRate);
    calculateSamplesPerSixteenth();
}

void DrumMachine::setEnabled(bool enabled) {
    if (enabled && !mEnabled) {
        // Starting playback - reset to start of measure
        mCurrentSixteenth = 0;
        mSampleCounter = 0.0f;
        triggerSixteenth(0);  // Trigger immediately on start
    }
    mEnabled = enabled;
}

void DrumMachine::setBPM(float bpm) {
    mBPM = std::max(60.0f, std::min(200.0f, bpm));
    calculateSamplesPerSixteenth();
}

void DrumMachine::setVolume(float volume) {
    mVolume = std::max(0.0f, std::min(1.0f, volume));
}

void DrumMachine::resetBeat() {
    mCurrentSixteenth = 0;
    mSampleCounter = 0.0f;
    triggerSixteenth(0);  // Trigger first 16th note immediately
}

void DrumMachine::calculateSamplesPerSixteenth() {
    // Samples per 16th note = samples per beat / 4
    // Samples per beat = (samples per second) / (beats per second)
    float samplesPerBeat = mSampleRate * 60.0f / mBPM;
    mSamplesPerSixteenth = samplesPerBeat / 4.0f;
}

void DrumMachine::triggerSixteenth(int sixteenth) {
    // Pattern: 
    // - Kick on beats 1,3 (16th notes 0, 8)
    // - Snare on beats 2,4 (16th notes 4, 12)
    // - Hi-hat on all 16th notes (if enabled)
    
    // Kick on downbeats of 1 and 3
    if (mKickEnabled && (sixteenth == 0 || sixteenth == 8)) {
        mDrumSynth.triggerKick();
    }
    
    // Snare on downbeats of 2 and 4
    if (mSnareEnabled && (sixteenth == 4 || sixteenth == 12)) {
        mDrumSynth.triggerSnare();
    }
    
    // Hi-hat with velocity variation
    if (mHiHatEnabled) {
        if (mHiHat16thNotes) {
            // 16th notes: play on every 16th note
            mDrumSynth.triggerHiHat(HIHAT_VELOCITIES[sixteenth]);
        } else {
            // 8th notes: play only on even 16th notes (0, 2, 4, 6, 8, 10, 12, 14)
            if (sixteenth % 2 == 0) {
                // Use velocity from corresponding position (doubled index maps to 8th note feel)
                float velocity = HIHAT_VELOCITIES[sixteenth];
                mDrumSynth.triggerHiHat(velocity);
            }
        }
    }
}

float DrumMachine::nextSample() {
    if (!mEnabled) {
        // Still process drum synth for any active sounds to decay
        return mDrumSynth.nextSample() * mVolume;
    }
    
    // Check if it's time for the next 16th note
    mSampleCounter += 1.0f;
    
    if (mSampleCounter >= mSamplesPerSixteenth) {
        mSampleCounter -= mSamplesPerSixteenth;  // Keep the fractional part for accuracy
        
        // Advance to next 16th note
        mCurrentSixteenth = (mCurrentSixteenth + 1) % 16;
        triggerSixteenth(mCurrentSixteenth);
    }
    
    // Get drum sample and apply volume
    return mDrumSynth.nextSample() * mVolume;
}

} // namespace synthio
