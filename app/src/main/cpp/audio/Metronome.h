#ifndef SYNTHIO_METRONOME_H
#define SYNTHIO_METRONOME_H

#include "DrumSynth.h"

namespace synthio {

/**
 * Simple metronome that uses the kick drum sound for reliable, audible clicks.
 * Used during loop recording to keep time without playing the full drum
 * pattern.
 */
class Metronome {
public:
  Metronome();

  void setSampleRate(float sampleRate);
  void setBPM(float bpm);

  // Control
  void start();
  void stop();
  bool isRunning() const { return mRunning; }

  // Get next sample - call every audio frame
  float nextSample();

  // Get current beat (0-3)
  int getCurrentBeat() const { return mCurrentBeat; }

  // Reset to beat 0
  void reset();

private:
  DrumSynth mDrumSynth; // Use kick drum for metronome sound

  float mSampleRate = 48000.0f;
  float mBPM = 100.0f;
  bool mRunning = false;

  // Timing
  int mCurrentBeat = 0;
  float mSampleCounter = 0.0f;
  float mSamplesPerBeat = 0.0f;

  // Volume for metronome clicks
  float mVolume = 0.9f;

  void setVolume(float volume) { mVolume = volume; }
  float getVolume() const { return mVolume; }

  void calculateTiming();
  void triggerClick();
};

} // namespace synthio

#endif // SYNTHIO_METRONOME_H
