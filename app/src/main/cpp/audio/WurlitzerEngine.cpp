#include "WurlitzerEngine.h"
#include <algorithm>
#include <cmath>

namespace synthio {

WurlitzerEngine::WurlitzerEngine() {
    // Set default effect parameters
    mTremolo.setRate(5.0f);
    mTremolo.setDepth(0.0f);  // Off by default
    mReverb.setSize(0.3f);
    mReverb.setMix(0.0f);     // Off by default
    mDelay.setTime(0.25f);
    mDelay.setMix(0.0f);      // Off by default
}

void WurlitzerEngine::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    
    for (auto& voice : mVoices) {
        voice.setSampleRate(sampleRate);
    }
    
    mTremolo.setSampleRate(sampleRate);
    mChorus.setSampleRate(sampleRate);
    mReverb.setSampleRate(sampleRate);
    mDelay.setSampleRate(sampleRate);
}

void WurlitzerEngine::noteOn(int midiNote, float frequency, float velocity) {
    // Check if note is already playing
    int existingVoice = findVoiceWithNote(midiNote);
    if (existingVoice >= 0) {
        mVoices[existingVoice].noteOn(midiNote, frequency, velocity);
        mVoiceAge[existingVoice] = ++mAgeCounter;
        return;
    }
    
    // Find free voice
    int voiceIndex = findFreeVoice();
    if (voiceIndex < 0) {
        voiceIndex = stealOldestVoice();
    }
    
    mVoices[voiceIndex].noteOn(midiNote, frequency, velocity);
    mVoiceAge[voiceIndex] = ++mAgeCounter;
}

void WurlitzerEngine::noteOff(int midiNote) {
    for (auto& voice : mVoices) {
        if (voice.getMidiNote() == midiNote && voice.isActive()) {
            voice.noteOff();
        }
    }
}

void WurlitzerEngine::allNotesOff() {
    for (auto& voice : mVoices) {
        if (voice.isActive()) {
            voice.noteOff();
        }
    }
}

void WurlitzerEngine::setTremoloRate(float rateHz) {
    mTremolo.setRate(rateHz);
}

void WurlitzerEngine::setTremoloDepth(float depth) {
    mTremolo.setDepth(depth);
}

void WurlitzerEngine::setChorusMode(int mode) {
    mChorus.setMode(static_cast<Chorus::Mode>(mode));
}

void WurlitzerEngine::setReverbSize(float size) {
    mReverb.setSize(size);
}

void WurlitzerEngine::setReverbMix(float mix) {
    mReverb.setMix(mix);
}

void WurlitzerEngine::setDelayTime(float time) {
    mDelay.setTime(time);
}

void WurlitzerEngine::setDelayFeedback(float feedback) {
    mDelay.setFeedback(feedback);
}

void WurlitzerEngine::setDelayMix(float mix) {
    mDelay.setMix(mix);
}

void WurlitzerEngine::setVolume(float volume) {
    mVolume = std::max(0.0f, std::min(1.0f, volume));
}

void WurlitzerEngine::process(float& outLeft, float& outRight) {
    // Sum all voices
    float sum = 0.0f;
    int activeCount = 0;
    
    for (auto& voice : mVoices) {
        if (voice.isActive()) {
            sum += voice.nextSample();
            activeCount++;
        }
    }
    
    // Auto-gain compensation for polyphony
    if (activeCount > 1) {
        sum /= std::sqrt(static_cast<float>(activeCount));
    }
    
    // Apply volume
    sum *= mVolume;
    
    // ===== EFFECT CHAIN (authentic Wurlitzer signal flow) =====
    
    // 1. TREMOLO first (this is THE signature Wurlitzer effect)
    //    Applied to mono signal before stereo processing
    float tremoloOut = mTremolo.process(sum);
    
    // 2. CHORUS creates stereo width from the tremolo'd mono signal
    mChorus.process(tremoloOut, outLeft, outRight);
    
    // 3. DELAY adds space and echo
    mDelay.process(outLeft, outRight);
    
    // 4. REVERB last for room ambience
    mReverb.process(outLeft, outRight);
    
    // Soft limit to prevent clipping
    outLeft = std::tanh(outLeft);
    outRight = std::tanh(outRight);
}

int WurlitzerEngine::findFreeVoice() {
    for (int i = 0; i < WURLI_MAX_VOICES; ++i) {
        if (!mVoices[i].isActive()) {
            return i;
        }
    }
    return -1;
}

int WurlitzerEngine::findVoiceWithNote(int midiNote) {
    for (int i = 0; i < WURLI_MAX_VOICES; ++i) {
        if (mVoices[i].isActive() && mVoices[i].getMidiNote() == midiNote) {
            return i;
        }
    }
    return -1;
}

int WurlitzerEngine::stealOldestVoice() {
    uint64_t oldestAge = UINT64_MAX;
    int oldestIndex = 0;
    
    for (int i = 0; i < WURLI_MAX_VOICES; ++i) {
        if (mVoiceAge[i] < oldestAge) {
            oldestAge = mVoiceAge[i];
            oldestIndex = i;
        }
    }
    
    return oldestIndex;
}

} // namespace synthio
