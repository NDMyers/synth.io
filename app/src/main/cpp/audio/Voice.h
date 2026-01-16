#ifndef SYNTHIO_VOICE_H
#define SYNTHIO_VOICE_H

#include "Envelope.h"
#include "Filter.h"
#include "Oscillator.h"
#include <random>

namespace synthio {

enum class VoiceState { IDLE, ACTIVE, RELEASING };

/**
 * Enhanced voice with Juno-106 style features:
 * - Sub-oscillator (square wave, one octave below)
 * - Noise generator
 * - Glide/Portamento
 * - LFO modulation inputs
 * - Key tracking for filter
 */
class Voice {
public:
  Voice();

  void setSampleRate(float sampleRate);

  // Note control
  void noteOn(int midiNote, float frequency);
  void noteOff();

  // Oscillator Parameters
  void setWaveform(Waveform waveform);
  void setWaveformEnabled(Waveform waveform, bool enabled);
  void setPulseWidth(float width); // For square wave PWM

  // Sub-oscillator
  void setSubOscLevel(float level); // 0.0 to 1.0

  // Noise generator
  void setNoiseLevel(float level); // 0.0 to 1.0

  // Filter parameters
  void setFilterCutoff(float cutoffHz);
  void setFilterResonance(float resonance);
  void setFilterEnvelopeAmount(float amount);
  void setFilterKeyTracking(float amount);
  void setHPFCutoff(float cutoffHz);

  // ADSR
  void setAttack(float time);
  void setDecay(float time);
  void setSustain(float level);
  void setRelease(float time);

  // Glide (Portamento)
  void setGlideTime(float time); // 0 = off, up to 2 seconds
  void setGlideEnabled(bool enabled);

  // LFO modulation inputs (applied from global LFO)
  void applyLFOPitchMod(float semitones);
  void applyLFOFilterMod(float amount); // -1 to 1
  void applyLFOPWMMod(float amount);    // -0.4 to 0.4

  // Unison detuning (for unison mode)
  void setDetune(float cents); // Detune amount in cents

  // Processing
  float nextSample();

  // State queries
  bool isActive() const { return mState != VoiceState::IDLE; }
  int getMidiNote() const { return mMidiNote; }
  VoiceState getState() const { return mState; }
  float getFrequency() const { return mTargetFrequency; }

private:
  // Oscillators
  Oscillator mOscillator;
  Oscillator mSubOscillator; // Sub-osc (always square, one octave below)

  // Filter and envelopes
  Filter mFilter;
  Envelope mAmpEnvelope;
  Envelope mFilterEnvelope;

  // Noise generator
  std::mt19937 mRng;
  std::uniform_real_distribution<float> mNoiseDist{-1.0f, 1.0f};
  float mNoiseLevel = 0.0f;

  // Sub-oscillator level
  float mSubOscLevel = 0.0f;

  // State
  VoiceState mState = VoiceState::IDLE;
  int mMidiNote = -1;
  float mSampleRate = 48000.0f;

  // Frequency and glide
  float mTargetFrequency = 440.0f;
  float mCurrentFrequency = 440.0f;
  float mGlideTime = 0.0f;
  float mGlideCoeff = 1.0f; // Exponential glide coefficient
  bool mGlideEnabled = false;
  bool mFirstNote = true;

  // Detune for unison
  float mDetuneRatio = 1.0f;

  // Filter modulation
  float mFilterBaseCutoff = 10000.0f;
  float mFilterEnvAmount = 0.0f;

  // LFO modulation values (set externally)
  float mLFOPitchMod = 0.0f;  // In semitones
  float mLFOFilterMod = 0.0f; // -1 to 1
  float mLFOPWMMod = 0.0f;    // -0.4 to 0.4
  float mBasePulseWidth = 0.5f;

  // Helper methods
  void updateGlideCoefficient();
  float generateNoise();
};

} // namespace synthio

#endif // SYNTHIO_VOICE_H
