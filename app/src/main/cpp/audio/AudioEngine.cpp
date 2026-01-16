#include "AudioEngine.h"
#include <algorithm>
#include <android/log.h>
#include <cmath>

#define LOG_TAG "SynthIO"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace synthio {

// =========================================================================
// TRUE LIMITER - Only compresses signals ABOVE threshold
// Signals below threshold pass through COMPLETELY UNCHANGED
// =========================================================================
struct TrueLimiter {
  float threshold = 0.9f;     // Level where limiting begins (-1dB)
  float attackCoeff = 0.001f; // Fast attack to catch peaks
  float releaseCoeff =
      0.0001f;           // Slow release to avoid pumping (~50ms at 48kHz)
  float envelope = 0.0f; // Current envelope level

  void setThreshold(float t) { threshold = t; }

  // Configure for sample rate (call once)
  void configure(float sampleRate, float attackMs, float releaseMs) {
    // Convert time to coefficient: coeff = 1 - e^(-1/(time * sampleRate))
    attackCoeff = 1.0f - std::exp(-1.0f / (attackMs * 0.001f * sampleRate));
    releaseCoeff = 1.0f - std::exp(-1.0f / (releaseMs * 0.001f * sampleRate));
  }

  float process(float input) {
    float absInput = std::abs(input);

    // Envelope follower with different attack/release
    if (absInput > envelope) {
      envelope += attackCoeff * (absInput - envelope); // Fast attack
    } else {
      envelope += releaseCoeff * (absInput - envelope); // Slow release
    }

    // BELOW THRESHOLD: pass through unchanged (key insight!)
    if (envelope <= threshold) {
      return input;
    }

    // ABOVE THRESHOLD: apply gain reduction
    float gainReduction = threshold / envelope;
    return input * gainReduction;
  }

  void reset() { envelope = 0.0f; }
};

// Global limiters for each signal path (persistent state across callbacks)
static TrueLimiter g_synthLimiterL;
static TrueLimiter g_synthLimiterR;
static TrueLimiter g_drumLimiter;
static TrueLimiter g_masterLimiterL;
static TrueLimiter g_masterLimiterR;
static bool g_limitersInitialized = false;

AudioEngine::AudioEngine() {
  mPolyphonyManager.setSampleRate(SAMPLE_RATE);
  mWurlitzerEngine.setSampleRate(SAMPLE_RATE);
  mDrumMachine.setSampleRate(SAMPLE_RATE);
  mLooper.setSampleRate(SAMPLE_RATE);
  mMetronome.setSampleRate(SAMPLE_RATE);

  // Initialize synth effects
  mSynthTremolo.setSampleRate(SAMPLE_RATE);
  mSynthDelay.setSampleRate(SAMPLE_RATE);
  mSynthReverb.setSampleRate(SAMPLE_RATE);

  // Set default values for synth effects (off by default)
  mSynthTremolo.setRate(5.0f);
  mSynthTremolo.setDepth(0.0f); // Off by default
  mSynthDelay.setTime(0.3f);
  mSynthDelay.setFeedback(0.3f);
  mSynthDelay.setMix(0.0f); // Off by default
  mSynthReverb.setSize(0.5f);
  mSynthReverb.setMix(0.0f); // Off by default
}

AudioEngine::~AudioEngine() { stop(); }

bool AudioEngine::start() {
  auto result = createStream();
  if (result != oboe::Result::OK) {
    LOGE("Failed to create audio stream: %s", oboe::convertToText(result));
    return false;
  }

  result = mStream->requestStart();
  if (result != oboe::Result::OK) {
    LOGE("Failed to start audio stream: %s", oboe::convertToText(result));
    return false;
  }

  LOGI("Audio engine started successfully");
  return true;
}

void AudioEngine::stop() {
  if (mStream) {
    mStream->requestStop();
    mStream->close();
    mStream.reset();
    LOGI("Audio engine stopped");
  }
}

void AudioEngine::restart() {
  // Prevent multiple simultaneous restarts
  bool expected = false;
  if (!mIsRestarting.compare_exchange_strong(expected, true)) {
    LOGI("Audio engine restart already in progress");
    return;
  }

  LOGI("Restarting audio engine for device change...");
  stop();
  start();

  mIsRestarting = false;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream *audioStream,
                                    oboe::Result error) {
  // This callback is triggered when the audio stream is disconnected
  // (e.g., Bluetooth device connected/disconnected, USB audio changes)
  LOGI("Audio stream disconnected (error: %s), restarting...",
       oboe::convertToText(error));

  // Restart the stream to connect to the new audio device
  // Use a small delay to allow the system to settle
  restart();
}

oboe::Result AudioEngine::createStream() {
  oboe::AudioStreamBuilder builder;

  builder.setDirection(oboe::Direction::Output)
      ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
      ->setSharingMode(
          oboe::SharingMode::Shared) // Shared mode for Bluetooth compatibility
      ->setFormat(oboe::AudioFormat::Float)
      ->setChannelCount(CHANNEL_COUNT)
      ->setSampleRate(SAMPLE_RATE)
      ->setUsage(oboe::Usage::Game) // Game audio gets lower latency treatment
      ->setContentType(oboe::ContentType::Music) // Mark as music content
      ->setDataCallback(this)
      ->setErrorCallback(this); // Handle device changes

  oboe::Result result = builder.openStream(mStream);

  if (result == oboe::Result::OK && mStream) {
    // Log the actual stream configuration for debugging latency
    int32_t bufferSize = mStream->getBufferSizeInFrames();
    int32_t framesPerBurst = mStream->getFramesPerBurst();
    int32_t sampleRate = mStream->getSampleRate();

    // Calculate approximate latency contribution from buffer
    double bufferLatencyMs = (bufferSize * 1000.0) / sampleRate;
    double burstLatencyMs = (framesPerBurst * 1000.0) / sampleRate;

    LOGI("Audio stream opened:");
    LOGI("  Sample rate: %d Hz", sampleRate);
    LOGI("  Buffer size: %d frames (%.1f ms)", bufferSize, bufferLatencyMs);
    LOGI("  Burst size: %d frames (%.1f ms)", framesPerBurst, burstLatencyMs);
    LOGI("  Sharing mode: %s",
         mStream->getSharingMode() == oboe::SharingMode::Exclusive ? "Exclusive"
                                                                   : "Shared");
    LOGI("  Performance mode: %s",
         mStream->getPerformanceMode() == oboe::PerformanceMode::LowLatency
             ? "LowLatency"
             : "Other");

    // Try to minimize buffer size for lowest latency
    // Set buffer to 2x burst size (minimum safe value)
    int32_t desiredBuffer = framesPerBurst * 2;
    if (desiredBuffer < bufferSize) {
      mStream->setBufferSizeInFrames(desiredBuffer);
      LOGI("  Reduced buffer to: %d frames (%.1f ms)", desiredBuffer,
           (desiredBuffer * 1000.0) / sampleRate);
    }
  }

  return result;
}

void AudioEngine::noteOn(int midiNote, float frequency) {
  std::lock_guard<std::mutex> lock(mMutex);
  if (mWurlitzerMode) {
    mWurlitzerEngine.noteOn(midiNote, frequency, 0.7f); // Default velocity
  } else {
    mPolyphonyManager.noteOn(midiNote, frequency);
  }
}

void AudioEngine::noteOn(int midiNote, float frequency, float velocity) {
  std::lock_guard<std::mutex> lock(mMutex);
  if (mWurlitzerMode) {
    mWurlitzerEngine.noteOn(midiNote, frequency, velocity);
  } else {
    mPolyphonyManager.noteOn(midiNote, frequency);
  }
}

void AudioEngine::noteOff(int midiNote) {
  std::lock_guard<std::mutex> lock(mMutex);
  if (mWurlitzerMode) {
    mWurlitzerEngine.noteOff(midiNote);
  } else {
    mPolyphonyManager.noteOff(midiNote);
  }
}

void AudioEngine::allNotesOff() {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.allNotesOff();
  mWurlitzerEngine.allNotesOff();
}

void AudioEngine::setWurlitzerMode(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  if (mWurlitzerMode != enabled) {
    mWurlitzerMode = enabled;
    // Kill all notes when switching modes to prevent stuck notes
    mPolyphonyManager.allNotesOff();
    mWurlitzerEngine.allNotesOff();
  }
}

// ===== OSCILLATOR PARAMETERS =====
void AudioEngine::setWaveform(int waveform) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setWaveform(static_cast<Waveform>(waveform));
}

void AudioEngine::toggleWaveform(int waveformId, bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setWaveformEnabled(static_cast<Waveform>(waveformId),
                                       enabled);
}

void AudioEngine::setPulseWidth(float width) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setPulseWidth(width);
}

void AudioEngine::setSubOscLevel(float level) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setSubOscLevel(level);
}

void AudioEngine::setNoiseLevel(float level) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setNoiseLevel(level);
}

// ===== FILTER PARAMETERS =====
void AudioEngine::setFilterCutoff(float cutoffHz) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setFilterCutoff(cutoffHz);
}

void AudioEngine::setFilterResonance(float resonance) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setFilterResonance(resonance);
}

void AudioEngine::setFilterEnvelopeAmount(float amount) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setFilterEnvelopeAmount(amount);
}

void AudioEngine::setFilterKeyTracking(float amount) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setFilterKeyTracking(amount);
}

void AudioEngine::setHPFCutoff(float cutoffHz) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setHPFCutoff(cutoffHz);
}

// ===== ENVELOPE (ADSR) =====
void AudioEngine::setAttack(float time) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setAttack(time);
}

void AudioEngine::setDecay(float time) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setDecay(time);
}

void AudioEngine::setSustain(float level) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setSustain(level);
}

void AudioEngine::setRelease(float time) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setRelease(time);
}

// ===== LFO PARAMETERS =====
void AudioEngine::setLFORate(float rateHz) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setLFORate(rateHz);
}

void AudioEngine::setLFOPitchDepth(float depth) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setLFOPitchDepth(depth);
}

void AudioEngine::setLFOFilterDepth(float depth) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setLFOFilterDepth(depth);
}

void AudioEngine::setLFOPWMDepth(float depth) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setLFOPWMDepth(depth);
}

// ===== CHORUS =====
void AudioEngine::setChorusMode(int mode) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setChorusMode(mode);
}

// ===== SYNTH EFFECTS (Delay, Reverb, Tremolo) =====
void AudioEngine::setSynthTremoloRate(float rate) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthTremolo.setRate(rate);
}

void AudioEngine::setSynthTremoloDepth(float depth) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthTremolo.setDepth(depth);
}

void AudioEngine::setSynthReverbSize(float size) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthReverb.setSize(size);
}

void AudioEngine::setSynthReverbMix(float mix) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthReverb.setMix(mix);
}

void AudioEngine::setSynthDelayTime(float time) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthDelay.setTime(time);
}

void AudioEngine::setSynthDelayFeedback(float feedback) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthDelay.setFeedback(feedback);
}

void AudioEngine::setSynthDelayMix(float mix) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthDelay.setMix(mix);
}

// ===== GLIDE/PORTAMENTO =====
void AudioEngine::setGlideTime(float time) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setGlideTime(time);
}

void AudioEngine::setGlideEnabled(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setGlideEnabled(enabled);
}

// ===== UNISON MODE =====
void AudioEngine::setUnisonEnabled(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setUnisonEnabled(enabled);
}

void AudioEngine::setUnisonVoices(int count) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setUnisonVoices(count);
}

void AudioEngine::setUnisonDetune(float cents) {
  std::lock_guard<std::mutex> lock(mMutex);
  mPolyphonyManager.setUnisonDetune(cents);
}

// ===== WURLITZER CONTROLS =====
void AudioEngine::setWurliTremoloRate(float rate) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setTremoloRate(rate);
}

void AudioEngine::setWurliTremoloDepth(float depth) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setTremoloDepth(depth);
}

void AudioEngine::setWurliChorusMode(int mode) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setChorusMode(mode);
}

void AudioEngine::setWurliReverbSize(float size) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setReverbSize(size);
}

void AudioEngine::setWurliReverbMix(float mix) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setReverbMix(mix);
}

void AudioEngine::setWurliDelayTime(float time) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setDelayTime(time);
}

void AudioEngine::setWurliDelayFeedback(float feedback) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setDelayFeedback(feedback);
}

void AudioEngine::setWurliDelayMix(float mix) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setDelayMix(mix);
}

void AudioEngine::setWurliVolume(float volume) {
  std::lock_guard<std::mutex> lock(mMutex);
  mWurlitzerEngine.setVolume(volume);
}

// Volume controls
void AudioEngine::setSynthVolume(float volume) {
  std::lock_guard<std::mutex> lock(mMutex);
  mSynthVolume = std::max(0.0f, std::min(1.0f, volume));
}

void AudioEngine::setDrumVolume(float volume) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setVolume(volume);
}

void AudioEngine::setMetronomeVolume(float volume) {
  std::lock_guard<std::mutex> lock(mMutex);
  mMetronomeVolume = std::max(0.0f, std::min(2.0f, volume));
}

// Drum machine controls
void AudioEngine::setDrumEnabled(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumEnabledByUser = enabled;

  // If enabling drums during loop playback, sync to loop position
  if (enabled && mLooper.getState() == Looper::State::PLAYING) {
    syncDrumToLoop();
  }

  mDrumMachine.setEnabled(enabled);
  LOGI("Drum machine %s (user: %s)", enabled ? "enabled" : "disabled",
       mDrumEnabledByUser ? "yes" : "no");
}

void AudioEngine::syncDrumToLoop() {
  // Sync drum machine beat position to match loop position
  // This ensures drums are on-beat with the loop
  if (mLooper.hasLoop() && mLooper.getLoopLengthSamples() > 0) {
    // The loop and drums should be at the same beat position
    // Reset drums to current loop beat
    mDrumMachine.resetBeat();
    LOGI("Drum machine synced to loop");
  }
}

void AudioEngine::setDrumBPM(float bpm) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setBPM(bpm);
  mLooper.setBPM(bpm);    // Keep looper in sync
  mMetronome.setBPM(bpm); // Keep metronome in sync
}

void AudioEngine::setKickEnabled(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setKickEnabled(enabled);
}

void AudioEngine::setSnareEnabled(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setSnareEnabled(enabled);
}

void AudioEngine::setHiHatEnabled(bool enabled) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setHiHatEnabled(enabled);
}

void AudioEngine::setHiHat16thNotes(bool is16th) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setHiHat16thNotes(is16th);
}

// ===== DRUM PATTERN CONTROLS =====

void AudioEngine::setDrumStep(int instrument, int step, float velocity) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setStep(instrument, step, velocity);
}

float AudioEngine::getDrumStep(int instrument, int step) const {
  // No lock needed for read-only
  return mDrumMachine.getStep(instrument, step);
}

void AudioEngine::toggleDrumStep(int instrument, int step) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.toggleStep(instrument, step);
}

void AudioEngine::setDrumInstrumentVolume(int instrument, float volume) {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.setInstrumentVolume(instrument, volume);
}

float AudioEngine::getDrumInstrumentVolume(int instrument) const {
  return mDrumMachine.getInstrumentVolume(instrument);
}

void AudioEngine::resetDrumPattern() {
  std::lock_guard<std::mutex> lock(mMutex);
  mDrumMachine.resetToDefaultPattern();
  LOGI("Drum pattern reset to default");
}

// ===== LOOPER CONTROLS =====
void AudioEngine::looperStartRecording() {
  std::lock_guard<std::mutex> lock(mMutex);

  // Sync looper and metronome BPM with drum machine
  float bpm = mDrumMachine.getBPM();
  mLooper.setBPM(bpm);
  mMetronome.setBPM(bpm);

  // Start metronome for the count-in (not drum machine)
  mMetronome.start();

  mLooper.startRecording();

  LOGI("Looper: Starting recording (pre-count) with metronome at %.1f BPM",
       bpm);
}

void AudioEngine::looperStartPlayback() {
  std::lock_guard<std::mutex> lock(mMutex);

  // Stop metronome if it was somehow still running
  mMetronome.stop();

  // If user has drums enabled, sync them to the loop start
  if (mDrumEnabledByUser) {
    mDrumMachine.resetBeat();
  }

  mLooper.startPlayback();
  LOGI("Looper: Starting playback (drums %s)",
       mDrumEnabledByUser ? "synced" : "off");
}

void AudioEngine::looperStopPlayback() {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.stopPlayback();
  mMetronome.stop(); // Ensure metronome is stopped
  LOGI("Looper: Stopped playback");
}

void AudioEngine::looperClearLoop() {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.clearLoop();
  mMetronome.stop();
  LOGI("Looper: Loop cleared");
}

int AudioEngine::getLooperState() const {
  return static_cast<int>(mLooper.getState());
}

bool AudioEngine::looperHasLoop() const { return mLooper.hasLoop(); }

int AudioEngine::getLooperCurrentBeat() const {
  return mLooper.getCurrentBeat();
}

int AudioEngine::getLooperCurrentBar() const { return mLooper.getCurrentBar(); }

void AudioEngine::looperStartRecordingTrack(int trackIndex) {
  std::lock_guard<std::mutex> lock(mMutex);

  // Sync BPM
  float bpm = mDrumMachine.getBPM();
  mLooper.setBPM(bpm);
  mMetronome.setBPM(bpm);

  // Start metronome for count-in
  mMetronome.start();

  mLooper.startRecordingTrack(trackIndex);
  LOGI("Looper: Starting recording track %d with metronome at %.1f BPM",
       trackIndex, bpm);
}

void AudioEngine::looperClearTrack(int trackIndex) {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.clearTrack(trackIndex);
  LOGI("Looper: Track %d cleared", trackIndex);
}

void AudioEngine::looperClearAllTracks() {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.clearAllTracks();
  mMetronome.stop();
  LOGI("Looper: All tracks cleared");
}

void AudioEngine::looperCancelRecording() {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.cancelRecording();
  mMetronome.stop(); // Stop metronome when canceling
  LOGI("Looper: Recording canceled");
}

void AudioEngine::looperSetTrackVolume(int trackIndex, float volume) {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.setTrackVolume(trackIndex, volume);
}

void AudioEngine::looperSetTrackMuted(int trackIndex, bool muted) {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.setTrackMuted(trackIndex, muted);
}

void AudioEngine::looperSetTrackSolo(int trackIndex, bool solo) {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.setTrackSolo(trackIndex, solo);
}

bool AudioEngine::looperTrackHasContent(int trackIndex) const {
  return mLooper.trackHasContent(trackIndex);
}

float AudioEngine::looperGetTrackVolume(int trackIndex) const {
  return mLooper.getTrackVolume(trackIndex);
}

bool AudioEngine::looperIsTrackMuted(int trackIndex) const {
  return mLooper.isTrackMuted(trackIndex);
}

bool AudioEngine::looperIsTrackSolo(int trackIndex) const {
  return mLooper.isTrackSolo(trackIndex);
}

int AudioEngine::looperGetActiveRecordingTrack() const {
  return mLooper.getActiveRecordingTrack();
}

int AudioEngine::looperGetUsedTrackCount() const {
  return mLooper.getUsedTrackCount();
}

void AudioEngine::looperSetBarCount(int bars) {
  std::lock_guard<std::mutex> lock(mMutex);
  mLooper.setBarCount(bars);
}

int AudioEngine::looperGetBarCount() const { return mLooper.getBarCount(); }

std::vector<float> AudioEngine::looperGetMixedBuffer(int trackMask) const {
  return mLooper.getMixedBuffer(trackMask);
}

int64_t AudioEngine::looperGetBufferSize() const {
  return mLooper.getLoopLengthSamples() * 2; // Stereo interleaved
}

oboe::DataCallbackResult
AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData,
                          int32_t numFrames) {
  // Clear buffer
  memset(audioData, 0, numFrames * CHANNEL_COUNT * sizeof(float));

  float *output = static_cast<float *>(audioData);

  for (int i = 0; i < numFrames; ++i) {
    float synthL = 0.0f;
    float synthR = 0.0f;

    // Render Synth or Wurlitzer (live input)
    if (mWurlitzerMode) {
      mWurlitzerEngine.process(synthL, synthR);
    } else {
      mPolyphonyManager.nextSample(synthL, synthR);

      // Apply synth effects chain: Tremolo -> Delay -> Reverb
      mSynthTremolo.process(synthL, synthR);
      mSynthDelay.process(synthL, synthR);
      mSynthReverb.process(synthL, synthR);

      // Bass boost: Simple low-shelf filter to enhance sub-200Hz frequencies
      // Using a one-pole lowpass to extract bass, then adding it back
      static float bassFilterL = 0.0f;
      static float bassFilterR = 0.0f;
      constexpr float BASS_CUTOFF = 0.02f;      // ~200Hz at 48kHz (lower = lower cutoff)
      constexpr float BASS_BOOST_AMOUNT = 0.4f; // ~3dB boost to low end

      // One-pole lowpass to extract bass
      bassFilterL += BASS_CUTOFF * (synthL - bassFilterL);
      bassFilterR += BASS_CUTOFF * (synthR - bassFilterR);

      // Add extracted bass back for boost
      synthL += bassFilterL * BASS_BOOST_AMOUNT;
      synthR += bassFilterR * BASS_BOOST_AMOUNT;
    }

    // Apply Master Volume to both Synth and Wurlitzer
    synthL *= mSynthVolume;
    synthR *= mSynthVolume;

    // Process looper - records synth audio and/or plays back loop
    float loopL = 0.0f;
    float loopR = 0.0f;
    mLooper.process(synthL, synthR, loopL, loopR);

    // Get looper state for audio routing decisions
    Looper::State looperState = mLooper.getState();

    // Metronome plays during pre-count and recording to provide timing
    // We use the drum machine's kick directly since it's proven to work
    float metronomeSample = 0.0f;
    bool needsMetronome = (looperState == Looper::State::PRE_COUNT ||
                           looperState == Looper::State::RECORDING);

    // Static variables for metronome timing (outside if/else for proper scope)
    static bool metronomeWasActive = false;
    static float metroSampleCounter = 0.0f;
    static int metroBeat = 0;
    static float metroSamplesPerBeat = 0.0f;

    if (needsMetronome) {
      // Initialize on first frame of metronome
      if (!metronomeWasActive) {
        metronomeWasActive = true;
        metroSampleCounter = 0.0f;
        metroBeat = 0;
        metroSamplesPerBeat = SAMPLE_RATE * 60.0f / mDrumMachine.getBPM();

        // Trigger first snare immediately (higher pitch than kick, cuts through
        // better)
        mDrumMachine.triggerSnare();
        LOGI("Metronome started via DrumMachine snare, BPM=%.1f",
             mDrumMachine.getBPM());
      }

      // Get the drum synth output
      // Note: Volume is applied later in mixing stage
      constexpr float METRONOME_VOLUME = 1.8f; // Loud metronome
      metronomeSample = mDrumMachine.getDrumSynthSample() * METRONOME_VOLUME;

      // Advance timing
      metroSampleCounter += 1.0f;
      if (metroSampleCounter >= metroSamplesPerBeat) {
        metroSampleCounter -= metroSamplesPerBeat;
        metroBeat = (metroBeat + 1) % 4;
        mDrumMachine.triggerSnare(); // Use snare for all metronome beats
        LOGI("Metronome beat %d", metroBeat);
      }
    } else {
      // Reset metronome state when not active
      metronomeWasActive = false;
    }

    // Drum machine plays only when:
    // 1. User has explicitly enabled it (mDrumEnabledByUser), OR
    // 2. During loop playback if user has drums enabled
    // NOT during pre-count or recording (metronome is used instead)
    float drumSample = 0.0f;
    bool shouldPlayDrums = mDrumEnabledByUser &&
                           looperState != Looper::State::PRE_COUNT &&
                           looperState != Looper::State::RECORDING;

    if (shouldPlayDrums) {
      drumSample = mDrumMachine.nextSample();
    }

    // =========== CLEAN GAIN STAGING (No Limiter) ===========
    // Best practice: Set gains so sum of ALL sources at MAX stays under 1.0
    // This gives linear volume response with no compression artifacts
    //
    // 2x overall volume boost for louder output
    // Drum ratio changed from 15x to 12x (synth more audible)
    // Metronome matches drum ratio for consistent volume
    //
    constexpr float SYNTH_GAIN = 0.09f; // 2x boost (was 0.045)
    constexpr float DRUM_GAIN = 1.08f;  // 12x relative to synth (was 15x/0.675)
    constexpr float METRO_GAIN =
        1.08f; // Same as drums for consistent metronome volume

    // Apply gain to each source - completely independent, no interaction
    float synthMixL = (synthL + loopL) * SYNTH_GAIN;
    float synthMixR = (synthR + loopR) * SYNTH_GAIN;
    float drumMix = drumSample * DRUM_GAIN;
    float metroMix = metronomeSample * mMetronomeVolume;

    // =========== SIMPLE SUM ===========
    // No limiter = no pumping, no ducking, no artifacts
    float finalL = synthMixL + drumMix + metroMix;
    float finalR = synthMixR + drumMix + metroMix;

    // =========== SAFETY CLIP ===========
    // Should rarely hit with proper gain staging
    // Using hard clip for clean, predictable behavior
    finalL = std::clamp(finalL, -1.0f, 1.0f);
    finalR = std::clamp(finalR, -1.0f, 1.0f);

    // Write to interleaved stereo buffer
    output[i * 2] = finalL;
    output[i * 2 + 1] = finalR;
  }

  return oboe::DataCallbackResult::Continue;
}

} // namespace synthio
