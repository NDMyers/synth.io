#define _USE_MATH_DEFINES
#include "DrumSynth.h"
#include <algorithm>
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

DrumSynth::DrumSynth() : mRng(std::random_device{}()) {}

void DrumSynth::setSampleRate(float sampleRate) { mSampleRate = sampleRate; }

void DrumSynth::triggerKick(float velocity) {
  mKick.active = true;
  // Velocity curve: exponential for more natural response
  mKick.velocity = std::pow(std::max(0.0f, std::min(1.0f, velocity)), 2.0f);
  mKick.phase = 0.0f;
  mKick.pitchEnv = 1.0f;
  mKick.ampEnv = 1.0f;
  mKick.sampleCount = 0;
}

void DrumSynth::triggerSnare(float velocity) {
  mSnare.active = true;
  mSnare.velocity = std::pow(std::max(0.0f, std::min(1.0f, velocity)), 2.0f);
  mSnare.bodyPhase = 0.0f;
  mSnare.toneEnv = 1.0f;
  mSnare.noiseEnv = 1.0f;
  mSnare.bpLow = 0.0f;
  mSnare.bpBand = 0.0f;
  mSnare.sampleCount = 0;
}

void DrumSynth::triggerHiHat(float velocity) {
  mHiHat.active = true;
  mHiHat.velocity =
      std::max(0.3f, std::min(1.0f, velocity)); // Clamp with minimum
  mHiHat.ampEnv = 1.0f;
  mHiHat.sampleCount = 0;
  // Reset oscillator phases for consistent attack
  for (int i = 0; i < 6; i++) {
    mHiHat.phases[i] = 0.0f;
  }
  mHiHat.hpState = 0.0f;
}

float DrumSynth::nextSample() {
  float output = 0.0f;

  if (mKick.active) {
    output += generateKickSample();
  }

  if (mSnare.active) {
    output += generateSnareSample();
  }

  if (mHiHat.active) {
    output += generateHiHatSample();
  }

  return output;
}

bool DrumSynth::isActive() const {
  return mKick.active || mSnare.active || mHiHat.active;
}

float DrumSynth::generateKickSample() {
  // Pitch envelope: exponential sweep from high to low
  float currentFreq =
      KickState::END_FREQ +
      (KickState::START_FREQ - KickState::END_FREQ) * mKick.pitchEnv;

  // Generate sine wave
  float sample = std::sin(mKick.phase * 2.0f * static_cast<float>(M_PI));

  // Tiny click at start
  float clickDurationSamples =
      (KickState::CLICK_DURATION_MS / 1000.0f) * mSampleRate;
  if (mKick.sampleCount < clickDurationSamples) {
    float clickEnv = 1.0f - (mKick.sampleCount / clickDurationSamples);
    sample += generateNoise() * clickEnv * 0.15f;
  }

  // Apply amplitude envelope
  sample *= mKick.ampEnv;

  // Advance phase
  mKick.phase += currentFreq / mSampleRate;
  if (mKick.phase >= 1.0f)
    mKick.phase -= 1.0f;

  // Decay envelopes (exponential)
  float sampleRateScale = mSampleRate / 48000.0f;
  mKick.pitchEnv *= (1.0f - KickState::PITCH_DECAY * sampleRateScale);
  mKick.ampEnv *= (1.0f - KickState::AMP_DECAY * sampleRateScale);

  mKick.sampleCount++;

  if (mKick.ampEnv < 0.001f) {
    mKick.active = false;
  }

  return sample * 1.0f * mKick.velocity; // Clean unity output with velocity
}

float DrumSynth::generateSnareSample() {
  // 707-style snare: mellow body tone + bandpass filtered noise

  // Body tone: sine wave around 200Hz for warmth
  float body = std::sin(mSnare.bodyPhase * 2.0f * static_cast<float>(M_PI));
  float toneSample = body * SnareState::BODY_MIX * mSnare.toneEnv;

  // Bandpass filtered noise using state variable filter
  // This creates the "snare rattle" without harsh high frequencies
  float rawNoise = generateNoise();

  // SVF bandpass filter coefficients
  float f = 2.0f * std::sin(static_cast<float>(M_PI) * SnareState::BP_FREQ /
                            mSampleRate);
  float q = 1.0f / SnareState::BP_Q;

  // State variable filter iteration
  mSnare.bpLow += f * mSnare.bpBand;
  float bpHigh = rawNoise - mSnare.bpLow - q * mSnare.bpBand;
  mSnare.bpBand += f * bpHigh;

  // Use bandpass output (mSnare.bpBand) for mellow snare character
  float noiseSample = mSnare.bpBand * SnareState::NOISE_MIX * mSnare.noiseEnv;

  // Mix tone and filtered noise
  float sample = toneSample + noiseSample;

  // Advance body phase
  mSnare.bodyPhase += SnareState::BODY_FREQ / mSampleRate;
  if (mSnare.bodyPhase >= 1.0f)
    mSnare.bodyPhase -= 1.0f;

  // Decay envelopes - tone decays faster than noise
  float sampleRateScale = mSampleRate / 48000.0f;
  mSnare.toneEnv *= (1.0f - SnareState::TONE_DECAY * sampleRateScale);
  mSnare.noiseEnv *= (1.0f - SnareState::NOISE_DECAY * sampleRateScale);

  mSnare.sampleCount++;

  // Cut off when both envelopes are done
  if (mSnare.toneEnv < 0.001f && mSnare.noiseEnv < 0.001f) {
    mSnare.active = false;
  }

  return sample * 1.0f * mSnare.velocity; // Clean unity output with velocity
}

float DrumSynth::generateHiHatSample() {
  // 707-style hi-hat: 6 square wave oscillators at inharmonic frequencies
  // Mixed with high-passed noise for sizzle

  float toneSum = 0.0f;

  // Sum 6 square wave oscillators at metallic frequencies
  for (int i = 0; i < 6; i++) {
    // Square wave: +1 or -1 based on phase
    float square = (mHiHat.phases[i] < 0.5f) ? 1.0f : -1.0f;
    toneSum += square;

    // Advance phase
    mHiHat.phases[i] += HiHatState::FREQS[i] / mSampleRate;
    if (mHiHat.phases[i] >= 1.0f)
      mHiHat.phases[i] -= 1.0f;
  }

  // Normalize the 6 oscillators
  toneSum /= 6.0f;

  // High-pass filter the tone for brightness (simple one-pole HP)
  float hpCoeff = 1.0f - std::exp(-2.0f * static_cast<float>(M_PI) *
                                  HiHatState::HP_FREQ / mSampleRate);
  mHiHat.hpState += hpCoeff * (toneSum - mHiHat.hpState);
  float filteredTone = toneSum - mHiHat.hpState;

  // Add high-frequency noise for sizzle
  float noise = generateNoise();
  // Simple HP on noise too (reuse coefficient)
  static float noiseHpState = 0.0f;
  noiseHpState += hpCoeff * (noise - noiseHpState);
  float filteredNoise = noise - noiseHpState;

  // Mix tone and noise
  float sample = filteredTone * HiHatState::TONE_MIX +
                 filteredNoise * HiHatState::NOISE_MIX;

  // Apply amplitude envelope with velocity
  sample *= mHiHat.ampEnv * mHiHat.velocity;

  // Decay envelope (exponential)
  float sampleRateScale = mSampleRate / 48000.0f;
  mHiHat.ampEnv *= (1.0f - HiHatState::AMP_DECAY * sampleRateScale);

  mHiHat.sampleCount++;

  // Cut off when envelope is done
  if (mHiHat.ampEnv < 0.001f) {
    mHiHat.active = false;
  }

  return sample * 0.175f; // Half volume relative to kick/snare
}

float DrumSynth::generateNoise() { return mNoiseDist(mRng); }

} // namespace synthio
