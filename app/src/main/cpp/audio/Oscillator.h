#ifndef SYNTHIO_OSCILLATOR_H
#define SYNTHIO_OSCILLATOR_H

#define _USE_MATH_DEFINES
#include <cmath>
#include <cstdint>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

enum class Waveform { SINE = 0, SQUARE = 1, SAWTOOTH = 2, TRIANGLE = 3 };

class Oscillator {
public:
  Oscillator();

  void setSampleRate(float sampleRate);
  void setFrequency(float frequency);
  void setWaveform(Waveform waveform); // Legacy override: Exclusive selection
  void setWaveformEnabled(Waveform waveform, bool enabled);
  void setPulseWidth(float pulseWidth); // 0.0 to 1.0

  float nextSample();
  void reset();

private:
  float mPhase = 0.0f;
  float mPhaseIncrement = 0.0f;
  float mFrequency = 440.0f;
  float mSampleRate = 48000.0f;
  float mPulseWidth = 0.5f;

  // Waveform state
  bool mEnabledWaveforms[4] = {true, false, false,
                               false}; // Default to SINE only

  void updatePhaseIncrement();

  // Waveform generators
  float generateSine();
  float generateSquare();
  float generateSawtooth();
  float generateTriangle();

  // PolyBLEP anti-aliasing
  float polyBlep(float t);
};

} // namespace synthio

#endif // SYNTHIO_OSCILLATOR_H
