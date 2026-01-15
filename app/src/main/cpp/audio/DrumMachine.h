#ifndef SYNTHIO_DRUM_MACHINE_H
#define SYNTHIO_DRUM_MACHINE_H

#include "DrumSynth.h"

namespace synthio {

/**
 * Simple 4/4 drum machine sequencer
 * Pattern: Kick on beats 1,3 / Snare on beats 2,4 / Hi-hat on 16th notes
 */
class DrumMachine {
public:
    DrumMachine();
    
    void setSampleRate(float sampleRate);
    
    // Enable/disable playback
    void setEnabled(bool enabled);
    bool isEnabled() const { return mEnabled; }
    
    // Hi-hat toggle
    void setHiHatEnabled(bool enabled) { mHiHatEnabled = enabled; }
    bool isHiHatEnabled() const { return mHiHatEnabled; }
    
    // Kick toggle
    void setKickEnabled(bool enabled) { mKickEnabled = enabled; }
    bool isKickEnabled() const { return mKickEnabled; }
    
    // Snare toggle
    void setSnareEnabled(bool enabled) { mSnareEnabled = enabled; }
    bool isSnareEnabled() const { return mSnareEnabled; }
    
    // Hi-hat mode: true = 16th notes, false = 8th notes
    void setHiHat16thNotes(bool is16th) { mHiHat16thNotes = is16th; }
    bool isHiHat16thNotes() const { return mHiHat16thNotes; }
    
    // BPM control (60-200)
    void setBPM(float bpm);
    float getBPM() const { return mBPM; }
    
    // Volume control (0.0 - 1.0)
    void setVolume(float volume);
    float getVolume() const { return mVolume; }
    
    // Reset beat position (for loop sync)
    void resetBeat();
    
    // Get next sample
    float nextSample();
    
    // Trigger sounds externally (for metronome use)
    void triggerKick() { mDrumSynth.triggerKick(); }
    void triggerSnare() { mDrumSynth.triggerSnare(); }
    void triggerHiHat(float velocity = 1.0f) { mDrumSynth.triggerHiHat(velocity); }
    
    // Get just the drum synth output (without advancing sequencer)
    float getDrumSynthSample() { return mDrumSynth.nextSample(); }

private:
    DrumSynth mDrumSynth;
    
    float mSampleRate = 48000.0f;
    bool mEnabled = false;
    bool mHiHatEnabled = false;   // Hi-hat toggle
    bool mKickEnabled = true;     // Kick toggle (on by default)
    bool mSnareEnabled = true;    // Snare toggle (on by default)
    bool mHiHat16thNotes = true;  // true = 16th notes, false = 8th notes
    float mBPM = 100.0f;
    float mVolume = 0.7f;
    
    // Sequencer state - now tracking 16th notes
    int mCurrentSixteenth = 0;   // 0-15 for 16th notes in a measure
    float mSampleCounter = 0.0f; // Samples since last 16th note
    float mSamplesPerSixteenth = 0.0f;
    
    // Velocity pattern for hi-hat human feel (accents on downbeats)
    static constexpr float HIHAT_VELOCITIES[16] = {
        1.0f,  0.5f,  0.7f,  0.4f,   // Beat 1: strong, weak, medium, weak
        0.9f,  0.5f,  0.6f,  0.4f,   // Beat 2: strong, weak, medium, weak
        1.0f,  0.5f,  0.7f,  0.4f,   // Beat 3: strong, weak, medium, weak
        0.9f,  0.5f,  0.6f,  0.45f   // Beat 4: strong, weak, medium, slightly stronger
    };
    
    void calculateSamplesPerSixteenth();
    void triggerSixteenth(int sixteenth);
};

} // namespace synthio

#endif // SYNTHIO_DRUM_MACHINE_H
