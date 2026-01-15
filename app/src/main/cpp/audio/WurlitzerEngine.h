#ifndef SYNTHIO_WURLITZER_ENGINE_H
#define SYNTHIO_WURLITZER_ENGINE_H

#include "WurlitzerVoice.h"
#include "Tremolo.h"
#include "Reverb.h"
#include "Delay.h"
#include "Chorus.h"
#include <array>
#include <cstdint>

namespace synthio {

constexpr int WURLI_MAX_VOICES = 12;

/**
 * Wurlitzer 200A polyphonic engine
 * Manages multiple WurlitzerVoices with built-in effects chain
 */
class WurlitzerEngine {
public:
    WurlitzerEngine();
    
    void setSampleRate(float sampleRate);
    
    // Note control
    void noteOn(int midiNote, float frequency, float velocity);
    void noteOff(int midiNote);
    void allNotesOff();
    
    // Effect controls
    void setTremoloRate(float rateHz);
    void setTremoloDepth(float depth);
    void setChorusMode(int mode);  // 0=off, 1=I, 2=II
    void setReverbSize(float size);
    void setReverbMix(float mix);
    void setDelayTime(float time);
    void setDelayFeedback(float feedback);
    void setDelayMix(float mix);
    void setVolume(float volume);
    
    // Audio processing - stereo output
    void process(float& outLeft, float& outRight);

private:
    std::array<WurlitzerVoice, WURLI_MAX_VOICES> mVoices;
    uint64_t mVoiceAge[WURLI_MAX_VOICES] = {0};
    uint64_t mAgeCounter = 0;
    
    // Effects chain
    Tremolo mTremolo;
    Chorus mChorus;
    Reverb mReverb;
    Delay mDelay;
    
    float mVolume = 0.7f;
    float mSampleRate = 48000.0f;
    
    int findFreeVoice();
    int findVoiceWithNote(int midiNote);
    int stealOldestVoice();
};

} // namespace synthio

#endif // SYNTHIO_WURLITZER_ENGINE_H
