#ifndef SYNTHIO_DRUM_SYNTH_H
#define SYNTHIO_DRUM_SYNTH_H

#include <cstdint>
#include <random>

namespace synthio {

/**
 * Classic 808/707-style drum synthesizer
 * Produces dry, punchy kick, snare, and metallic hi-hat sounds
 */
class DrumSynth {
public:
    DrumSynth();
    
    void setSampleRate(float sampleRate);
    
    // Trigger drum hits
    void triggerKick();
    void triggerSnare();
    void triggerHiHat(float velocity = 1.0f);  // velocity 0-1 for human feel
    
    // Get next sample (mix of all active drum voices)
    float nextSample();
    
    // Check if any drums are currently sounding
    bool isActive() const;

private:
    float mSampleRate = 48000.0f;
    
    // Random generator for noise
    std::mt19937 mRng;
    std::uniform_real_distribution<float> mNoiseDist{-1.0f, 1.0f};
    
    // ========== KICK DRUM ==========
    // Punchy 808-style kick with controlled decay
    struct KickState {
        bool active = false;
        float phase = 0.0f;
        float pitchEnv = 0.0f;
        float ampEnv = 0.0f;
        int sampleCount = 0;
        
        static constexpr float START_FREQ = 150.0f;   // Classic start freq
        static constexpr float END_FREQ = 55.0f;      // Low fundamental
        static constexpr float PITCH_DECAY = 0.0008f; // Smooth pitch sweep
        static constexpr float AMP_DECAY = 0.00005f;  // Medium decay - punchy but present
        static constexpr float CLICK_DURATION_MS = 2.0f;
    } mKick;
    
    // ========== SNARE DRUM ==========
    // 707-style snare: mellow body tone + bandpass filtered noise
    // The real 707 uses 8-bit samples, this emulates its character
    struct SnareState {
        bool active = false;
        float bodyPhase = 0.0f;       // Body tone oscillator
        float toneEnv = 0.0f;         // Tone envelope (fast decay)
        float noiseEnv = 0.0f;        // Noise envelope (slightly longer)
        int sampleCount = 0;
        
        // Bandpass filter state for noise (2-pole)
        float bpLow = 0.0f;
        float bpBand = 0.0f;
        
        // 707-style parameters - mellow, rounded, punchy
        static constexpr float BODY_FREQ = 200.0f;       // Body tone ~200Hz
        static constexpr float TONE_DECAY = 0.00035f;    // Fast tone decay
        static constexpr float NOISE_DECAY = 0.00045f;   // Moderate noise decay
        static constexpr float BODY_MIX = 0.85f;         // Body tone level
        static constexpr float NOISE_MIX = 0.15f;        // Filtered noise level
        static constexpr float BP_FREQ = 3500.0f;        // Bandpass center (mid-range)
        static constexpr float BP_Q = 0.7f;              // Bandpass resonance (gentle)
    } mSnare;
    
    // ========== HI-HAT ==========
    // 707-style closed hi-hat: 6 square wave oscillators at inharmonic frequencies
    // Creates metallic, shimmering tone characteristic of analog drum machines
    struct HiHatState {
        bool active = false;
        float phases[6] = {0};     // 6 oscillator phases
        float ampEnv = 0.0f;       // Amplitude envelope
        float velocity = 1.0f;     // Hit velocity for human feel
        int sampleCount = 0;
        
        // High-pass filter state
        float hpState = 0.0f;
        
        // 707-style inharmonic frequencies (creates metallic timbre)
        // These ratios are based on classic analog drum machine designs
        static constexpr float FREQS[6] = {
            205.3f,    // Fundamental-ish
            369.6f,    // ~1.8x (inharmonic)
            304.4f,    // ~1.5x
            522.7f,    // ~2.5x
            800.0f,    // Higher metallic
            1127.0f    // Highest shimmer
        };
        
        static constexpr float AMP_DECAY = 0.0006f;    // Faster decay for dead/tight hi-hat
        static constexpr float HP_FREQ = 7000.0f;      // High-pass cutoff for brightness
        static constexpr float TONE_MIX = 0.6f;        // Square wave metallic tone
        static constexpr float NOISE_MIX = 0.4f;       // Filtered noise for sizzle
    } mHiHat;
    
    // Helper methods
    float generateKickSample();
    float generateSnareSample();
    float generateHiHatSample();
    float generateNoise();
};

} // namespace synthio

#endif // SYNTHIO_DRUM_SYNTH_H
