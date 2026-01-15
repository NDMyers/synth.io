#include "Metronome.h"
#include <cmath>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "SynthIO-Metronome"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace synthio {

Metronome::Metronome() {
    calculateTiming();
}

void Metronome::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    mDrumSynth.setSampleRate(sampleRate);
    calculateTiming();
}

void Metronome::setBPM(float bpm) {
    mBPM = std::max(30.0f, std::min(300.0f, bpm));
    calculateTiming();
    LOGI("Metronome BPM set to %.1f", mBPM);
}

void Metronome::calculateTiming() {
    mSamplesPerBeat = mSampleRate * 60.0f / mBPM;
}

void Metronome::start() {
    LOGI("Metronome::start() - BPM=%.1f, sampleRate=%.0f, samplesPerBeat=%.0f", 
         mBPM, mSampleRate, mSamplesPerBeat);
    
    mRunning = true;
    mCurrentBeat = 0;
    mSampleCounter = 0.0f;
    
    // Trigger first click immediately
    triggerClick();
    
    LOGI("Metronome started, first kick triggered, mRunning=%d", mRunning);
}

void Metronome::stop() {
    LOGI("Metronome::stop()");
    mRunning = false;
}

void Metronome::reset() {
    mCurrentBeat = 0;
    mSampleCounter = 0.0f;
}

void Metronome::triggerClick() {
    // Use kick drum for a reliable, audible click
    mDrumSynth.triggerKick();
    LOGI("Metronome KICK on beat %d", mCurrentBeat);
}

float Metronome::nextSample() {
    // Always get the drum synth sample (for decay of active sounds)
    float output = mDrumSynth.nextSample() * CLICK_VOLUME;
    
    // Debug: log periodically to verify we're being called
    static int callCount = 0;
    static float maxOutput = 0.0f;
    callCount++;
    if (output > maxOutput) maxOutput = output;
    
    if (callCount % 48000 == 0) {  // Log once per second
        LOGI("Metronome::nextSample() called %d times, mRunning=%d, maxOutput=%.4f, beat=%d", 
             callCount, mRunning, maxOutput, mCurrentBeat);
        maxOutput = 0.0f;
    }
    
    // Handle timing if running - advance to next beat
    if (mRunning) {
        mSampleCounter += 1.0f;
        
        if (mSampleCounter >= mSamplesPerBeat) {
            mSampleCounter -= mSamplesPerBeat;
            
            // Advance to next beat
            mCurrentBeat = (mCurrentBeat + 1) % 4;
            triggerClick();
        }
    }
    
    return output;
}

} // namespace synthio
