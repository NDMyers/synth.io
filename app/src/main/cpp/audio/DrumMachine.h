#ifndef SYNTHIO_DRUM_MACHINE_H
#define SYNTHIO_DRUM_MACHINE_H

#include "DrumSynth.h"
#include <array>

namespace synthio {

/**
 * DAW-style drum machine sequencer with customizable patterns.
 *
 * Features:
 * - 16-step patterns per instrument (one bar of 16th notes)
 * - Per-step velocity control
 * - Per-instrument volume control
 * - Default pattern: Kick on 1,3 / Snare on 2,4 / Hi-hat on 16ths
 */
class DrumMachine {
public:
  // Instrument indices
  static constexpr int KICK = 0;
  static constexpr int SNARE = 1;
  static constexpr int HIHAT = 2;
  static constexpr int NUM_INSTRUMENTS = 3;
  static constexpr int NUM_STEPS = 16;

  DrumMachine();

  void setSampleRate(float sampleRate);

  // Enable/disable playback
  void setEnabled(bool enabled);
  bool isEnabled() const { return mEnabled; }

  // Legacy toggles (still control whether instrument sounds at all)
  void setHiHatEnabled(bool enabled) { mHiHatEnabled = enabled; }
  bool isHiHatEnabled() const { return mHiHatEnabled; }
  void setKickEnabled(bool enabled) { mKickEnabled = enabled; }
  bool isKickEnabled() const { return mKickEnabled; }
  void setSnareEnabled(bool enabled) { mSnareEnabled = enabled; }
  bool isSnareEnabled() const { return mSnareEnabled; }

  // Legacy hi-hat mode (kept for backward compat, but pattern overrides)
  void setHiHat16thNotes(bool is16th) { mHiHat16thNotes = is16th; }
  bool isHiHat16thNotes() const { return mHiHat16thNotes; }

  // BPM control (60-200)
  void setBPM(float bpm);
  float getBPM() const { return mBPM; }

  // Master volume control (0.0 - 1.0)
  void setVolume(float volume);
  float getVolume() const { return mVolume; }

  // ===== PATTERN CONTROL =====

  // Set step velocity (0.0 = off, 0.0-1.0 = velocity)
  void setStep(int instrument, int step, float velocity);
  float getStep(int instrument, int step) const;

  // Toggle step on/off with default velocity
  void toggleStep(int instrument, int step);

  // Per-instrument volume (0.0 - 1.0)
  void setInstrumentVolume(int instrument, float volume);
  float getInstrumentVolume(int instrument) const;

  // Reset to default pattern (kick 1,3 / snare 2,4 / hihat 16ths)
  void resetToDefaultPattern();

  // Get pattern arrays for UI sync
  const std::array<float, NUM_STEPS> &getKickPattern() const {
    return mKickPattern;
  }
  const std::array<float, NUM_STEPS> &getSnarePattern() const {
    return mSnarePattern;
  }
  const std::array<float, NUM_STEPS> &getHiHatPattern() const {
    return mHiHatPattern;
  }

  // Reset beat position (for loop sync)
  void resetBeat();

  // Get next sample
  float nextSample();

  // Trigger sounds externally (for metronome use)
  void triggerKick() { mDrumSynth.triggerKick(); }
  void triggerSnare() { mDrumSynth.triggerSnare(); }
  void triggerHiHat(float velocity = 1.0f) {
    mDrumSynth.triggerHiHat(velocity);
  }

  // Get just the drum synth output (without advancing sequencer)
  float getDrumSynthSample() { return mDrumSynth.nextSample(); }

private:
  DrumSynth mDrumSynth;

  float mSampleRate = 48000.0f;
  bool mEnabled = false;
  bool mHiHatEnabled = false;  // Master toggle for hi-hat
  bool mKickEnabled = true;    // Master toggle for kick
  bool mSnareEnabled = true;   // Master toggle for snare
  bool mHiHat16thNotes = true; // Legacy flag (pattern takes priority)
  float mBPM = 100.0f;
  float mVolume = 0.7f; // Master drum volume

  // ===== PATTERN DATA =====
  // 16 steps per instrument, values 0.0-1.0 (0 = off, >0 = velocity)
  std::array<float, NUM_STEPS> mKickPattern;
  std::array<float, NUM_STEPS> mSnarePattern;
  std::array<float, NUM_STEPS> mHiHatPattern;

  // Per-instrument volumes
  float mKickVolume = 1.0f;
  float mSnareVolume = 1.0f;
  float mHiHatVolume = 1.0f;

  // Sequencer state
  int mCurrentSixteenth = 0;   // 0-15 for 16th notes in a measure
  float mSampleCounter = 0.0f; // Samples since last 16th note
  float mSamplesPerSixteenth = 0.0f;

  void calculateSamplesPerSixteenth();
  void triggerSixteenth(int sixteenth);
  bool isValidInstrument(int instrument) const {
    return instrument >= 0 && instrument < NUM_INSTRUMENTS;
  }
  bool isValidStep(int step) const { return step >= 0 && step < NUM_STEPS; }
};

} // namespace synthio

#endif // SYNTHIO_DRUM_MACHINE_H
