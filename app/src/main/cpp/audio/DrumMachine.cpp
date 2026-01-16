#include "DrumMachine.h"
#include <algorithm>

namespace synthio {

DrumMachine::DrumMachine() {
  resetToDefaultPattern();
  calculateSamplesPerSixteenth();
}

void DrumMachine::setSampleRate(float sampleRate) {
  mSampleRate = sampleRate;
  mDrumSynth.setSampleRate(sampleRate);
  calculateSamplesPerSixteenth();
}

void DrumMachine::setEnabled(bool enabled) {
  if (enabled && !mEnabled) {
    // Starting playback - reset to start of measure
    mCurrentSixteenth = 0;
    mSampleCounter = 0.0f;
    triggerSixteenth(0); // Trigger immediately on start
  }
  mEnabled = enabled;
}

void DrumMachine::setBPM(float bpm) {
  mBPM = std::max(60.0f, std::min(200.0f, bpm));
  calculateSamplesPerSixteenth();
}

void DrumMachine::setVolume(float volume) {
  mVolume = std::max(0.0f, std::min(1.0f, volume));
}

// ===== PATTERN CONTROL =====

void DrumMachine::setStep(int instrument, int step, float velocity) {
  if (!isValidInstrument(instrument) || !isValidStep(step))
    return;

  float clampedVel = std::max(0.0f, std::min(1.0f, velocity));

  switch (instrument) {
  case KICK:
    mKickPattern[step] = clampedVel;
    break;
  case SNARE:
    mSnarePattern[step] = clampedVel;
    break;
  case HIHAT:
    mHiHatPattern[step] = clampedVel;
    break;
  }
}

float DrumMachine::getStep(int instrument, int step) const {
  if (!isValidInstrument(instrument) || !isValidStep(step))
    return 0.0f;

  switch (instrument) {
  case KICK:
    return mKickPattern[step];
  case SNARE:
    return mSnarePattern[step];
  case HIHAT:
    return mHiHatPattern[step];
  default:
    return 0.0f;
  }
}

void DrumMachine::toggleStep(int instrument, int step) {
  if (!isValidInstrument(instrument) || !isValidStep(step))
    return;

  float currentVel = getStep(instrument, step);
  // Toggle: if off (0), set to full velocity (1.0); if on, turn off
  float newVel = (currentVel > 0.0f) ? 0.0f : 1.0f;
  setStep(instrument, step, newVel);
}

void DrumMachine::setInstrumentVolume(int instrument, float volume) {
  if (!isValidInstrument(instrument))
    return;

  float clampedVol = std::max(0.0f, std::min(1.0f, volume));

  switch (instrument) {
  case KICK:
    mKickVolume = clampedVol;
    break;
  case SNARE:
    mSnareVolume = clampedVol;
    break;
  case HIHAT:
    mHiHatVolume = clampedVol;
    break;
  }
}

float DrumMachine::getInstrumentVolume(int instrument) const {
  if (!isValidInstrument(instrument))
    return 0.0f;

  switch (instrument) {
  case KICK:
    return mKickVolume;
  case SNARE:
    return mSnareVolume;
  case HIHAT:
    return mHiHatVolume;
  default:
    return 0.0f;
  }
}

void DrumMachine::resetToDefaultPattern() {
  // Clear all patterns
  mKickPattern.fill(0.0f);
  mSnarePattern.fill(0.0f);
  mHiHatPattern.fill(0.0f);

  // Default kick: beats 1 and 3 (16th notes 0 and 8)
  mKickPattern[0] = 1.0f;
  mKickPattern[8] = 1.0f;

  // Default snare: beats 2 and 4 (16th notes 4 and 12)
  mSnarePattern[4] = 1.0f;
  mSnarePattern[12] = 1.0f;

  // Default hi-hat: all 16th notes with velocity variation for groove
  // Strong on downbeats, weaker on offbeats
  mHiHatPattern[0] = 1.0f; // Beat 1: strong
  mHiHatPattern[1] = 0.5f;
  mHiHatPattern[2] = 0.7f;
  mHiHatPattern[3] = 0.4f;
  mHiHatPattern[4] = 0.9f; // Beat 2: strong
  mHiHatPattern[5] = 0.5f;
  mHiHatPattern[6] = 0.6f;
  mHiHatPattern[7] = 0.4f;
  mHiHatPattern[8] = 1.0f; // Beat 3: strong
  mHiHatPattern[9] = 0.5f;
  mHiHatPattern[10] = 0.7f;
  mHiHatPattern[11] = 0.4f;
  mHiHatPattern[12] = 0.9f; // Beat 4: strong
  mHiHatPattern[13] = 0.5f;
  mHiHatPattern[14] = 0.6f;
  mHiHatPattern[15] = 0.45f;

  // Reset per-instrument volumes to full
  mKickVolume = 1.0f;
  mSnareVolume = 1.0f;
  mHiHatVolume = 1.0f;
}

void DrumMachine::resetBeat() {
  mCurrentSixteenth = 0;
  mSampleCounter = 0.0f;
  triggerSixteenth(0); // Trigger first 16th note immediately
}

void DrumMachine::calculateSamplesPerSixteenth() {
  // Samples per 16th note = samples per beat / 4
  // Samples per beat = (samples per second) / (beats per second)
  float samplesPerBeat = mSampleRate * 60.0f / mBPM;
  mSamplesPerSixteenth = samplesPerBeat / 4.0f;
}

void DrumMachine::triggerSixteenth(int sixteenth) {
  if (sixteenth < 0 || sixteenth >= NUM_STEPS)
    return;

  // Kick - read from pattern array
  if (mKickEnabled && mKickPattern[sixteenth] > 0.0f) {
    // DrumSynth kick now takes velocity
    float velocity = mKickPattern[sixteenth] * mKickVolume;
    mDrumSynth.triggerKick(velocity);
  }

  // Snare - read from pattern array
  if (mSnareEnabled && mSnarePattern[sixteenth] > 0.0f) {
    float velocity = mSnarePattern[sixteenth] * mSnareVolume;
    mDrumSynth.triggerSnare(velocity);
  }

  // Hi-hat - read from pattern array, uses velocity for expression
  if (mHiHatEnabled && mHiHatPattern[sixteenth] > 0.0f) {
    float velocity = mHiHatPattern[sixteenth] * mHiHatVolume;
    mDrumSynth.triggerHiHat(velocity);
  }
}

float DrumMachine::nextSample() {
  if (!mEnabled) {
    // Still process drum synth for any active sounds to decay
    return mDrumSynth.nextSample() * mVolume;
  }

  // Check if it's time for the next 16th note
  mSampleCounter += 1.0f;

  if (mSampleCounter >= mSamplesPerSixteenth) {
    mSampleCounter -=
        mSamplesPerSixteenth; // Keep the fractional part for accuracy

    // Advance to next 16th note
    mCurrentSixteenth = (mCurrentSixteenth + 1) % NUM_STEPS;
    triggerSixteenth(mCurrentSixteenth);
  }

  // Get drum sample and apply master volume
  return mDrumSynth.nextSample() * mVolume;
}

} // namespace synthio
