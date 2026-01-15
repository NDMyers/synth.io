#ifndef SYNTHIO_WURLITZER_VOICE_H
#define SYNTHIO_WURLITZER_VOICE_H

#include <cstdint>

namespace synthio {

/**
 * Physical modeling-inspired Wurlitzer 200A voice emulation
 * 
 * The Wurlitzer 200A creates sound through:
 * - Metal reeds struck by felt hammers
 * - Electrostatic pickup sensing reed vibration
 * - Internal amplifier with characteristic coloration
 * 
 * Sonic character (smooth & buttery):
 * - Warm, mellow fundamental tone
 * - Gentle attack with soft transients
 * - Rich, bell-like sustain
 * - Relaxing, vintage electric piano feel
 * - Subtle velocity sensitivity
 */
class WurlitzerVoice {
public:
    WurlitzerVoice();
    
    void setSampleRate(float sampleRate);
    
    void noteOn(int midiNote, float frequency, float velocity);
    void noteOff();
    
    float nextSample();
    
    bool isActive() const;
    int getMidiNote() const { return mMidiNote; }
    
private:
    float mSampleRate = 48000.0f;
    int mMidiNote = -1;
    float mFrequency = 440.0f;
    float mVelocity = 0.7f;
    bool mNoteOn = false;
    bool mActive = false;
    
    // ===== OSCILLATOR PHASES =====
    float mPhase1 = 0.0f;      // Fundamental
    float mPhase2 = 0.0f;      // 2nd harmonic (octave)
    float mPhase3 = 0.0f;      // 3rd harmonic (crucial for Wurlitzer character)
    float mPhase4 = 0.0f;      // FM modulator for "bark"
    float mPhaseNoise = 0.0f;  // Attack transient noise
    
    // ===== ENVELOPE SYSTEM =====
    struct Envelope {
        float level = 0.0f;
        float attackRate = 0.0f;
        float decayRate = 0.0f;
        float sustainLevel = 0.0f;
        float releaseRate = 0.0f;
        bool attacking = false;
        bool decaying = false;
        bool sustaining = false;
        bool releasing = false;
        
        void trigger(float attack, float decay, float sustain, float release, float sr);
        void release();
        float process();
        bool isActive() const;
    };
    
    Envelope mAmpEnv;      // Main amplitude envelope
    Envelope mBarkEnv;     // Attack "bark" envelope (very fast decay)
    Envelope mHarmonicEnv; // Higher harmonic decay envelope
    Envelope mTineEnv;     // Tine/reed resonance envelope
    
    // ===== VOICE PARAMETERS (velocity-modulated) =====
    float mFundamentalLevel = 0.6f;
    float mSecondHarmonicLevel = 0.25f;
    float mThirdHarmonicLevel = 0.15f;
    float mBarkIntensity = 0.0f;
    
    // ===== FEEDBACK & STATE =====
    float mFeedback = 0.0f;
    float mLastSample = 0.0f;
    float mDCBlocker = 0.0f;    // DC offset removal
    
    // ===== PHYSICAL MODEL PARAMETERS =====
    // Tuned for smooth, warm Wurlitzer character
    static constexpr float TINE_INHARMONICITY = 1.0005f;  // Very slight pitch stretch
    static constexpr float FEEDBACK_AMOUNT = 0.08f;       // Gentle self-modulation
    
    // ===== HELPER FUNCTIONS =====
    float sine(float phase);
    float softClip(float x);
    void setupEnvelopes(float velocity);
    float generateHammerNoise();
};

} // namespace synthio

#endif // SYNTHIO_WURLITZER_VOICE_H
