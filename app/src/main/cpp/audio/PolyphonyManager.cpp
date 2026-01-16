#include "PolyphonyManager.h"
#include <algorithm>
#include <cmath>

namespace synthio {

PolyphonyManager::PolyphonyManager() {
  for (auto &voice : mVoices) {
    applyParamsToVoice(voice);
  }
  // Initialize unison tracking
  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    mUnisonNoteVoices[i] = -1;
  }
}

void PolyphonyManager::setSampleRate(float sampleRate) {
  for (auto &voice : mVoices) {
    voice.setSampleRate(sampleRate);
  }
  mLFO.setSampleRate(sampleRate);
  mChorus.setSampleRate(sampleRate);
}

void PolyphonyManager::noteOn(int midiNote, float frequency) {
  if (mUnisonEnabled) {
    noteOnUnison(midiNote, frequency);
    return;
  }

  // First check if this note is already playing, if so, retrigger it
  int existingVoice = findVoiceWithNote(midiNote);
  if (existingVoice >= 0) {
    mVoices[existingVoice].noteOn(midiNote, frequency);
    mVoiceAge[existingVoice] = ++mAgeCounter;
    return;
  }

  // Find a free voice
  int voiceIndex = findFreeVoice();
  if (voiceIndex < 0) {
    // No free voice, steal the oldest one
    voiceIndex = stealOldestVoice();
  }

  applyParamsToVoice(mVoices[voiceIndex]);
  mVoices[voiceIndex].setDetune(0.0f); // No detune in normal mode
  mVoices[voiceIndex].noteOn(midiNote, frequency);
  mVoiceAge[voiceIndex] = ++mAgeCounter;
}

void PolyphonyManager::noteOff(int midiNote) {
  if (mUnisonEnabled) {
    noteOffUnison(midiNote);
    return;
  }

  for (auto &voice : mVoices) {
    if (voice.getMidiNote() == midiNote &&
        voice.getState() == VoiceState::ACTIVE) {
      voice.noteOff();
    }
  }
}

void PolyphonyManager::allNotesOff() {
  for (auto &voice : mVoices) {
    if (voice.isActive()) {
      voice.noteOff();
    }
  }
  // Reset unison tracking
  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    mUnisonNoteVoices[i] = -1;
  }
}

// ===== OSCILLATOR PARAMETERS =====
void PolyphonyManager::setWaveform(Waveform waveform) {
  // Exclusive mode logic for legacy support
  for (int i = 0; i < 4; ++i) {
    mEnabledWaveforms[i] = false;
  }
  mEnabledWaveforms[static_cast<int>(waveform)] = true;

  for (auto &voice : mVoices) {
    voice.setWaveform(waveform); // Use voice's exclusive setter
  }
}

void PolyphonyManager::setWaveformEnabled(Waveform waveform, bool enabled) {
  mEnabledWaveforms[static_cast<int>(waveform)] = enabled;
  for (auto &voice : mVoices) {
    voice.setWaveformEnabled(waveform, enabled);
  }
}

void PolyphonyManager::setPulseWidth(float width) {
  mPulseWidth = width;
  for (auto &voice : mVoices) {
    voice.setPulseWidth(width);
  }
}

void PolyphonyManager::setSubOscLevel(float level) {
  mSubOscLevel = level;
  for (auto &voice : mVoices) {
    voice.setSubOscLevel(level);
  }
}

void PolyphonyManager::setNoiseLevel(float level) {
  mNoiseLevel = level;
  for (auto &voice : mVoices) {
    voice.setNoiseLevel(level);
  }
}

// ===== FILTER PARAMETERS =====
void PolyphonyManager::setFilterCutoff(float cutoffHz) {
  mFilterCutoff = cutoffHz;
  for (auto &voice : mVoices) {
    voice.setFilterCutoff(cutoffHz);
  }
}

void PolyphonyManager::setFilterResonance(float resonance) {
  mFilterResonance = resonance;
  for (auto &voice : mVoices) {
    voice.setFilterResonance(resonance);
  }
}

void PolyphonyManager::setFilterEnvelopeAmount(float amount) {
  mFilterEnvAmount = amount;
  for (auto &voice : mVoices) {
    voice.setFilterEnvelopeAmount(amount);
  }
}

void PolyphonyManager::setFilterKeyTracking(float amount) {
  mFilterKeyTracking = amount;
  for (auto &voice : mVoices) {
    voice.setFilterKeyTracking(amount);
  }
}

void PolyphonyManager::setHPFCutoff(float cutoffHz) {
  mHPFCutoff = cutoffHz;
  for (auto &voice : mVoices) {
    voice.setHPFCutoff(cutoffHz);
  }
}

// ===== ENVELOPE (ADSR) =====
void PolyphonyManager::setAttack(float time) {
  mAttack = time;
  for (auto &voice : mVoices) {
    voice.setAttack(time);
  }
}

void PolyphonyManager::setDecay(float time) {
  mDecay = time;
  for (auto &voice : mVoices) {
    voice.setDecay(time);
  }
}

void PolyphonyManager::setSustain(float level) {
  mSustain = level;
  for (auto &voice : mVoices) {
    voice.setSustain(level);
  }
}

void PolyphonyManager::setRelease(float time) {
  mRelease = time;
  for (auto &voice : mVoices) {
    voice.setRelease(time);
  }
}

// ===== LFO PARAMETERS =====
void PolyphonyManager::setLFORate(float rateHz) { mLFO.setRate(rateHz); }

void PolyphonyManager::setLFOPitchDepth(float depth) {
  mLFO.setPitchDepth(depth);
}

void PolyphonyManager::setLFOFilterDepth(float depth) {
  mLFO.setFilterDepth(depth);
}

void PolyphonyManager::setLFOPWMDepth(float depth) { mLFO.setPWMDepth(depth); }

// ===== CHORUS =====
void PolyphonyManager::setChorusMode(int mode) {
  mChorus.setMode(static_cast<Chorus::Mode>(mode));
}

// ===== GLIDE/PORTAMENTO =====
void PolyphonyManager::setGlideTime(float time) {
  mGlideTime = time;
  for (auto &voice : mVoices) {
    voice.setGlideTime(time);
  }
}

void PolyphonyManager::setGlideEnabled(bool enabled) {
  mGlideEnabled = enabled;
  for (auto &voice : mVoices) {
    voice.setGlideEnabled(enabled);
  }
}

// ===== UNISON MODE =====
void PolyphonyManager::setUnisonEnabled(bool enabled) {
  if (mUnisonEnabled != enabled) {
    // When toggling unison, release all notes
    allNotesOff();
  }
  mUnisonEnabled = enabled;
}

void PolyphonyManager::setUnisonVoices(int count) {
  mUnisonVoices = std::max(1, std::min(8, count));
}

void PolyphonyManager::setUnisonDetune(float cents) {
  mUnisonDetune = std::max(0.0f, std::min(50.0f, cents));
}

void PolyphonyManager::setMasterGain(float gain) {
  mMasterGain = std::max(0.0f, std::min(1.0f, gain));
}

// ===== UNISON HELPERS =====
void PolyphonyManager::noteOnUnison(int midiNote, float frequency) {
  // Find how many voices we can allocate for this note
  int voicesToUse = std::min(mUnisonVoices, MAX_POLYPHONY);

  // First, check if this note is already playing in unison
  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    if (mVoices[i].getMidiNote() == midiNote && mVoices[i].isActive()) {
      // Retrigger existing unison voices for this note
      for (int j = 0; j < MAX_POLYPHONY; ++j) {
        if (mVoices[j].getMidiNote() == midiNote) {
          mVoices[j].noteOn(midiNote, frequency);
          mVoiceAge[j] = ++mAgeCounter;
        }
      }
      return;
    }
  }

  // Allocate new voices for unison
  int allocatedCount = 0;
  for (int v = 0; v < voicesToUse; ++v) {
    int voiceIndex = findFreeVoice();
    if (voiceIndex < 0) {
      voiceIndex = stealOldestVoice();
    }

    applyParamsToVoice(mVoices[voiceIndex]);

    // Apply detune spread
    float detune = calculateUnisonDetune(v, voicesToUse);
    mVoices[voiceIndex].setDetune(detune);

    mVoices[voiceIndex].noteOn(midiNote, frequency);
    mVoiceAge[voiceIndex] = ++mAgeCounter;
    mUnisonNoteVoices[voiceIndex] = midiNote;
    allocatedCount++;
  }
}

void PolyphonyManager::noteOffUnison(int midiNote) {
  // Release all voices playing this note
  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    if (mVoices[i].getMidiNote() == midiNote &&
        mVoices[i].getState() == VoiceState::ACTIVE) {
      mVoices[i].noteOff();
      mUnisonNoteVoices[i] = -1;
    }
  }
}

float PolyphonyManager::calculateUnisonDetune(int voiceIndex, int totalVoices) {
  if (totalVoices <= 1)
    return 0.0f;

  // Spread voices evenly across the detune range
  // Center voice (if odd number) is at 0
  float spread = mUnisonDetune;
  float step = spread * 2.0f / static_cast<float>(totalVoices - 1);
  return -spread + step * static_cast<float>(voiceIndex);
}

// ===== AUDIO PROCESSING =====
void PolyphonyManager::applyLFOToVoices() {
  // Get LFO modulation values
  float pitchMod = mLFO.getPitchMod();
  float filterMod = mLFO.getFilterMod();
  float pwmMod = mLFO.getPWMMod();

  // Apply to all active voices
  for (auto &voice : mVoices) {
    if (voice.isActive()) {
      voice.applyLFOPitchMod(pitchMod);
      voice.applyLFOFilterMod(filterMod);
      voice.applyLFOPWMMod(pwmMod);
    }
  }
}

int PolyphonyManager::countActiveVoices() {
  int count = 0;
  for (const auto &voice : mVoices) {
    if (voice.isActive()) {
      count++;
    }
  }
  return count;
}

void PolyphonyManager::nextSample(float &outLeft, float &outRight) {
  // Advance LFO and apply modulation
  mLFO.tick();
  applyLFOToVoices();

  // Sum all voice outputs
  float sum = 0.0f;
  int activeCount = 0;

  for (auto &voice : mVoices) {
    if (voice.isActive()) {
      sum += voice.nextSample();
      activeCount++;
    }
  }

  // Calculate target auto-gain based on active voice count
  float targetAutoGain = 1.0f;
  if (activeCount > 1) {
    targetAutoGain = 1.0f / std::sqrt(static_cast<float>(activeCount));
  }

  // Smooth the auto-gain
  mCurrentAutoGain = mCurrentAutoGain * mAutoGainSmoothing +
                     targetAutoGain * (1.0f - mAutoGainSmoothing);

  // Apply gains
  sum *= mCurrentAutoGain * mMasterGain;

  // Apply soft limiter
  sum = softLimit(sum);

  // Apply chorus (stereo effect)
  mChorus.process(sum, outLeft, outRight);
}

float PolyphonyManager::nextSample() {
  // Legacy mono output - mix stereo to mono
  float left, right;
  nextSample(left, right);
  return (left + right) * 0.5f;
}

float PolyphonyManager::softLimit(float sample) {
  const float threshold = 0.8f;
  const float knee = 0.2f;

  float absSample = std::abs(sample);

  if (absSample <= threshold) {
    return sample;
  } else if (absSample <= threshold + knee) {
    float excess = absSample - threshold;
    float compressed = threshold + excess * (1.0f - excess / (2.0f * knee));
    return (sample > 0) ? compressed : -compressed;
  } else {
    float excess = absSample - threshold - knee;
    float limited = threshold + knee * 0.5f +
                    (1.0f - threshold - knee * 0.5f) * std::tanh(excess * 2.0f);
    return (sample > 0) ? limited : -limited;
  }
}

int PolyphonyManager::findFreeVoice() {
  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    if (!mVoices[i].isActive()) {
      return i;
    }
  }
  return -1;
}

int PolyphonyManager::findVoiceWithNote(int midiNote) {
  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    if (mVoices[i].getMidiNote() == midiNote && mVoices[i].isActive()) {
      return i;
    }
  }
  return -1;
}

int PolyphonyManager::stealOldestVoice() {
  uint64_t minAge = UINT64_MAX;
  int oldestIndex = 0;

  for (int i = 0; i < MAX_POLYPHONY; ++i) {
    if (mVoiceAge[i] < minAge) {
      minAge = mVoiceAge[i];
      oldestIndex = i;
    }
  }

  return oldestIndex;
}

void PolyphonyManager::applyParamsToVoice(Voice &voice) {
  // Apply all enabled waveforms
  for (int i = 0; i < 4; ++i) {
    voice.setWaveformEnabled(static_cast<Waveform>(i), mEnabledWaveforms[i]);
  }
  voice.setPulseWidth(mPulseWidth);
  voice.setSubOscLevel(mSubOscLevel);
  voice.setNoiseLevel(mNoiseLevel);
  voice.setFilterCutoff(mFilterCutoff);
  voice.setFilterResonance(mFilterResonance);
  voice.setFilterEnvelopeAmount(mFilterEnvAmount);
  voice.setFilterKeyTracking(mFilterKeyTracking);
  voice.setHPFCutoff(mHPFCutoff);
  voice.setAttack(mAttack);
  voice.setDecay(mDecay);
  voice.setSustain(mSustain);
  voice.setRelease(mRelease);
  voice.setGlideTime(mGlideTime);
  voice.setGlideEnabled(mGlideEnabled);
}

} // namespace synthio
