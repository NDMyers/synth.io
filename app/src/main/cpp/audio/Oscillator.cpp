#include "Oscillator.h"
#include <algorithm>

namespace synthio {

constexpr float TWO_PI = 2.0f * M_PI;

Oscillator::Oscillator() { updatePhaseIncrement(); }

void Oscillator::setSampleRate(float sampleRate) {
  mSampleRate = sampleRate;
  updatePhaseIncrement();
}

void Oscillator::setFrequency(float frequency) {
  mFrequency = frequency;
  updatePhaseIncrement();
}

void Oscillator::setWaveform(Waveform waveform) {
  // Legacy support: Exclusive selection
  for (int i = 0; i < 4; ++i) {
    mEnabledWaveforms[i] = false;
  }
  mEnabledWaveforms[static_cast<int>(waveform)] = true;
}

void Oscillator::setWaveformEnabled(Waveform waveform, bool enabled) {
  mEnabledWaveforms[static_cast<int>(waveform)] = enabled;
}

void Oscillator::setPulseWidth(float pulseWidth) {
  mPulseWidth = std::max(0.01f, std::min(0.99f, pulseWidth));
}

void Oscillator::updatePhaseIncrement() {
  mPhaseIncrement = mFrequency / mSampleRate;
}

void Oscillator::reset() { mPhase = 0.0f; }

float Oscillator::nextSample() {
  float sample = 0.0f;

  // Sum active waveforms
  int activeCount = 0;

  if (mEnabledWaveforms[static_cast<int>(Waveform::SINE)]) {
    sample += generateSine();
    activeCount++;
  }
  if (mEnabledWaveforms[static_cast<int>(Waveform::SQUARE)]) {
    sample += generateSquare();
    activeCount++;
  }
  if (mEnabledWaveforms[static_cast<int>(Waveform::SAWTOOTH)]) {
    sample += generateSawtooth();
    activeCount++;
  }
  if (mEnabledWaveforms[static_cast<int>(Waveform::TRIANGLE)]) {
    sample += generateTriangle();
    activeCount++;
  }

  // SAFEGUARD: Gain Staging & Limiting
  // When layering waveforms, amplitudes add up. We need to control the output
  // to prevent harsh digital clipping while maintaining the "fat" sound of
  // layers.
  if (activeCount > 1) {
    // 1. Power Normalization: 1/sqrt(N)
    // This keeps the perceived loudness increase natural (3dB increase per
    // doublng) rather than linear (6dB).
    sample /= std::sqrt(static_cast<float>(activeCount));

    // 2. Soft Clipper (Saturation)
    // Industry standard "analog" behavior. Instead of hard clipping at 1.0,
    // we saturate using tanh function. This adds pleasant harmonics when driven
    // hot.
    sample = std::tanh(sample * 1.1f); // Mild drive
  }

  // Advance phase
  mPhase += mPhaseIncrement;
  if (mPhase >= 1.0f) {
    mPhase -= 1.0f;
  }

  return sample;
}

float Oscillator::generateSine() { return std::sin(mPhase * TWO_PI); }

float Oscillator::generateSquare() {
  float sample = (mPhase < mPulseWidth) ? 1.0f : -1.0f;

  // Apply PolyBLEP anti-aliasing at discontinuities
  sample += polyBlep(mPhase);
  sample -= polyBlep(fmod(mPhase - mPulseWidth + 1.0f, 1.0f));

  return sample;
}

float Oscillator::generateSawtooth() {
  float sample = 2.0f * mPhase - 1.0f;

  // Apply PolyBLEP anti-aliasing
  sample -= polyBlep(mPhase);

  return sample;
}

float Oscillator::generateTriangle() {
  // Triangle can be derived from integrated square wave
  // Or phase-based computation
  float sample;
  if (mPhase < 0.5f) {
    sample = 4.0f * mPhase - 1.0f;
  } else {
    sample = 3.0f - 4.0f * mPhase;
  }
  return sample;
}

// PolyBLEP (Polynomial Band-Limited Step) anti-aliasing
// Reduces aliasing artifacts at waveform discontinuities
float Oscillator::polyBlep(float t) {
  float dt = mPhaseIncrement;

  // Near the start of the cycle
  if (t < dt) {
    t /= dt;
    return t + t - t * t - 1.0f;
  }
  // Near the end of the cycle
  else if (t > 1.0f - dt) {
    t = (t - 1.0f) / dt;
    return t * t + t + t + 1.0f;
  }

  return 0.0f;
}

} // namespace synthio
