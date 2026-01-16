#ifndef SYNTHIO_LOOPER_H
#define SYNTHIO_LOOPER_H

#include <array>
#include <cstdint>
#include <functional>
#include <vector>

namespace synthio {

/**
 * Multi-track audio looper with 4 synchronized tracks.
 *
 * Features:
 * - 4 concurrent loop tracks, all synced to the same timing
 * - Per-track volume, mute, and solo controls
 * - 4-beat pre-count before recording
 * - 4-bar loop recording per track
 * - Records synth audio only (not drums)
 * - Can hear existing tracks while recording a new one
 */
class Looper {
public:
  static constexpr int MAX_TRACKS = 4;
  static constexpr int PRE_COUNT_BEATS = 4;
  static constexpr int MIN_BARS = 1;
  static constexpr int MAX_BARS = 8;
  static constexpr int DEFAULT_BARS = 4;
  static constexpr int BEATS_PER_BAR = 4;

  enum class State {
    IDLE,      // No loops, ready to record
    PRE_COUNT, // Counting down before recording
    RECORDING, // Recording to active track
    STOPPED,   // Has loop(s), not playing
    PLAYING    // Playing back loops
  };

  // Individual track data
  struct LoopTrack {
    std::vector<float> bufferL;
    std::vector<float> bufferR;
    bool hasContent = false;
    float volume = 0.7f;
    bool muted = false;
    bool solo = false;
  };

  // Callback for state changes (to notify UI)
  using StateCallback = std::function<void(State, int)>; // state, beat number

  Looper();

  void setSampleRate(float sampleRate);
  void setBPM(float bpm);

  // ===== BAR COUNT CONFIGURATION =====
  void setBarCount(int bars); // Set number of bars per loop (1-8)
  int getBarCount() const { return mBarsToRecord; }

  // ===== MAIN CONTROL =====
  void startRecording(); // Start recording track 0 (backward compat)
  void startRecordingTrack(int trackIndex); // Start recording specific track
  void stopPlayback();                      // Stop playing all loops
  void startPlayback();                     // Start playing all loops
  void clearLoop();                         // Clear all loops (backward compat)
  void cancelRecording(); // Cancel current recording prematurely

  // ===== TRACK CONTROLS =====
  void setTrackVolume(int trackIndex, float volume);
  void setTrackMuted(int trackIndex, bool muted);
  void setTrackSolo(int trackIndex, bool solo);
  void clearTrack(int trackIndex);
  void clearAllTracks();

  // ===== STATE QUERIES =====
  State getState() const { return mState; }
  bool hasLoop() const { return hasAnyLoop(); } // Backward compat
  bool hasAnyLoop() const;
  bool isRecording() const { return mState == State::RECORDING; }
  bool isPlaying() const { return mState == State::PLAYING; }
  bool isPreCounting() const { return mState == State::PRE_COUNT; }
  int getCurrentBeat() const { return mCurrentBeat; }
  int getCurrentBar() const { return mCurrentBar; }

  // ===== TRACK QUERIES =====
  bool trackHasContent(int trackIndex) const;
  float getTrackVolume(int trackIndex) const;
  bool isTrackMuted(int trackIndex) const;
  bool isTrackSolo(int trackIndex) const;
  int getActiveRecordingTrack() const { return mActiveRecordingTrack; }
  int getUsedTrackCount() const;

  // ===== AUDIO PROCESSING =====
  // inputL/R = synth audio to potentially record
  // Returns the mixed loop playback to add to output
  void process(float synthL, float synthR, float &loopOutL, float &loopOutR);

  // ===== SYNC INFO =====
  int64_t getPlaybackPosition() const { return mPlaybackPosition; }
  int64_t getLoopLengthSamples() const { return mLoopLengthSamples; }

  // ===== AUDIO EXPORT =====
  // Get raw track buffer data for export
  const float *getTrackBufferL(int trackIndex) const;
  const float *getTrackBufferR(int trackIndex) const;
  int64_t getTrackBufferSize(int trackIndex) const;

  // Get mixed stereo buffer (interleaved L/R) for specified tracks
  // trackMask: bitmask of tracks to include (bit 0 = track 0, etc.)
  // includeDrums is handled at Kotlin level
  std::vector<float> getMixedBuffer(int trackMask) const;

  // Set callback for state changes
  void setStateCallback(StateCallback callback) { mStateCallback = callback; }

private:
  State mState = State::IDLE;
  float mSampleRate = 48000.0f;
  float mBPM = 100.0f;
  int mBarsToRecord = DEFAULT_BARS; // Configurable bar count

  // Multi-track storage
  std::array<LoopTrack, MAX_TRACKS> mTracks;
  int mActiveRecordingTrack = -1; // -1 = not recording any track

  // Timing (shared by all tracks)
  int mSamplesPerBeat = 0;
  int mSamplesPerBar = 0;
  int64_t mLoopLengthSamples = 0;
  bool mLoopLengthLocked = false; // True once first loop is recorded

  // Position tracking (shared by all tracks)
  int64_t mRecordPosition = 0;
  int64_t mPlaybackPosition = 0;
  int64_t mPreCountPosition = 0;

  // Beat/bar tracking for UI feedback
  int mCurrentBeat = 0;
  int mCurrentBar = 0;

  StateCallback mStateCallback = nullptr;

  void updateTiming();
  void updateBeatBar();
  void notifyStateChange();
  bool anySolo() const; // Returns true if any track has solo enabled
  bool isValidTrackIndex(int index) const {
    return index >= 0 && index < MAX_TRACKS;
  }
};

} // namespace synthio

#endif // SYNTHIO_LOOPER_H
