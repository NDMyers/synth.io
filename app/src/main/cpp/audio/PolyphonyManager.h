#ifndef SYNTHIO_POLYPHONY_MANAGER_H
#define SYNTHIO_POLYPHONY_MANAGER_H

#include "Voice.h"
#include "LFO.h"
#include "Chorus.h"
#include <array>
#include <cstdint>

namespace synthio {

constexpr int MAX_POLYPHONY = 12;  // 12 voices for playing 7th chords comfortably

/**
 * Enhanced polyphony manager with Juno-106 style features:
 * - Global LFO with modulation routing
 * - Stereo chorus effect
 * - Unison mode with voice stacking and detuning
 */
class PolyphonyManager {
public:
    PolyphonyManager();
    
    void setSampleRate(float sampleRate);
    
    // Note control
    void noteOn(int midiNote, float frequency);
    void noteOff(int midiNote);
    void allNotesOff();
    
    // ===== OSCILLATOR PARAMETERS =====
    void setWaveform(Waveform waveform);
    void setPulseWidth(float width);
    void setSubOscLevel(float level);
    void setNoiseLevel(float level);
    
    // ===== FILTER PARAMETERS =====
    void setFilterCutoff(float cutoffHz);
    void setFilterResonance(float resonance);
    void setFilterEnvelopeAmount(float amount);
    void setFilterKeyTracking(float amount);
    void setHPFCutoff(float cutoffHz);
    
    // ===== ENVELOPE (ADSR) =====
    void setAttack(float time);
    void setDecay(float time);
    void setSustain(float level);
    void setRelease(float time);
    
    // ===== LFO PARAMETERS =====
    void setLFORate(float rateHz);
    void setLFOPitchDepth(float depth);
    void setLFOFilterDepth(float depth);
    void setLFOPWMDepth(float depth);
    
    // ===== CHORUS =====
    void setChorusMode(int mode);  // 0=off, 1=Mode I, 2=Mode II
    
    // ===== GLIDE/PORTAMENTO =====
    void setGlideTime(float time);
    void setGlideEnabled(bool enabled);
    
    // ===== UNISON MODE =====
    void setUnisonEnabled(bool enabled);
    void setUnisonVoices(int count);    // 1-8 voices
    void setUnisonDetune(float cents);  // Spread in cents
    
    // Master gain control
    void setMasterGain(float gain);
    
    // Audio processing - returns stereo pair
    void nextSample(float& outLeft, float& outRight);
    
    // Legacy mono output (for compatibility)
    float nextSample();
    
private:
    std::array<Voice, MAX_POLYPHONY> mVoices;
    uint64_t mVoiceAge[MAX_POLYPHONY] = {0};
    uint64_t mAgeCounter = 0;
    
    // Global LFO
    LFO mLFO;
    
    // Stereo chorus
    Chorus mChorus;
    
    // Current parameters
    Waveform mCurrentWaveform = Waveform::SAWTOOTH;
    float mPulseWidth = 0.5f;
    float mSubOscLevel = 0.0f;
    float mNoiseLevel = 0.0f;
    float mFilterCutoff = 10000.0f;
    float mFilterResonance = 0.0f;
    float mFilterEnvAmount = 0.3f;
    float mFilterKeyTracking = 0.0f;
    float mHPFCutoff = 0.0f;
    float mAttack = 0.01f;
    float mDecay = 0.2f;
    float mSustain = 0.7f;
    float mRelease = 0.3f;
    float mGlideTime = 0.0f;
    bool mGlideEnabled = false;
    
    // Unison parameters
    bool mUnisonEnabled = false;
    int mUnisonVoices = 4;
    float mUnisonDetune = 10.0f;  // cents
    
    // For unison mode: track which voices belong to which note
    int mUnisonNoteVoices[MAX_POLYPHONY] = {-1};
    
    // Gain control
    float mMasterGain = 0.7f;
    float mCurrentAutoGain = 1.0f;
    float mAutoGainSmoothing = 0.9995f;
    
    int findFreeVoice();
    int findVoiceWithNote(int midiNote);
    int stealOldestVoice();
    int countActiveVoices();
    
    void applyParamsToVoice(Voice& voice);
    void applyLFOToVoices();
    
    // Unison helpers
    void noteOnUnison(int midiNote, float frequency);
    void noteOffUnison(int midiNote);
    float calculateUnisonDetune(int voiceIndex, int totalVoices);
    
    // Soft limiter to prevent clipping
    float softLimit(float sample);
};

} // namespace synthio

#endif // SYNTHIO_POLYPHONY_MANAGER_H
