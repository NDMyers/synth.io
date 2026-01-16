#ifndef SYNTHIO_AUDIO_ENGINE_H
#define SYNTHIO_AUDIO_ENGINE_H

#include "Delay.h"
#include "DrumMachine.h"
#include "Looper.h"
#include "Metronome.h"
#include "PolyphonyManager.h"
#include "Reverb.h"
#include "Tremolo.h"
#include "WurlitzerEngine.h"
#include <atomic>
#include <mutex>
#include <oboe/Oboe.h>

namespace synthio {

/**
 * Main audio engine using Oboe for low-latency audio output.
 * Supports Bluetooth audio devices through shared mode and proper audio
 * attributes. Handles audio device changes (connect/disconnect) automatically.
 */
class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
  AudioEngine();
  ~AudioEngine();

  // Lifecycle
  bool start();
  void stop();
  void restart(); // Restart audio stream (e.g., after device change)

  // Note control
  void noteOn(int midiNote, float frequency);
  void noteOn(int midiNote, float frequency,
              float velocity); // Velocity-sensitive version
  void noteOff(int midiNote);
  void allNotesOff();

  // ===== MODE SWITCHING =====
  void setWurlitzerMode(bool enabled);
  bool isWurlitzerMode() const { return mWurlitzerMode; }

  // ===== OSCILLATOR PARAMETERS =====
  void setWaveform(int waveform);
  void toggleWaveform(int waveformId, bool enabled);
  void setPulseWidth(float width);
  void setSubOscLevel(float level);
  void setNoiseLevel(float level);

  // ===== FILTER PARAMETERS =====
  void setFilterCutoff(float cutoffHz);
  void setFilterResonance(float resonance);
  void setFilterEnvelopeAmount(float amount);
  void setFilterKeyTracking(float amount);
  void setHPFCutoff(float cutoffHz);

  // ===== ENVELOPE (ADSR) =====
  void setAttack(float time);
  void setDecay(float time);
  void setSustain(float level);
  void setRelease(float time);

  // ===== LFO PARAMETERS =====
  void setLFORate(float rateHz);
  void setLFOPitchDepth(float depth);
  void setLFOFilterDepth(float depth);
  void setLFOPWMDepth(float depth);

  // ===== CHORUS =====
  void setChorusMode(int mode);

  // ===== SYNTH EFFECTS (Delay, Reverb, Tremolo) =====
  void setSynthTremoloRate(float rate);
  void setSynthTremoloDepth(float depth);
  void setSynthReverbSize(float size);
  void setSynthReverbMix(float mix);
  void setSynthDelayTime(float time);
  void setSynthDelayFeedback(float feedback);
  void setSynthDelayMix(float mix);

  // ===== GLIDE/PORTAMENTO =====
  void setGlideTime(float time);
  void setGlideEnabled(bool enabled);

  // ===== UNISON MODE =====
  void setUnisonEnabled(bool enabled);
  void setUnisonVoices(int count);
  void setUnisonDetune(float cents);

  // ===== WURLITZER CONTROLS =====
  void setWurliTremoloRate(float rate);
  void setWurliTremoloDepth(float depth);
  void setWurliChorusMode(int mode);
  void setWurliReverbSize(float size);
  void setWurliReverbMix(float mix);
  void setWurliDelayTime(float time);
  void setWurliDelayFeedback(float feedback);
  void setWurliDelayMix(float mix);
  void setWurliVolume(float volume);

  // Volume controls
  void setSynthVolume(float volume);
  void setDrumVolume(float volume);
  void setMetronomeVolume(float volume);

  // Drum machine controls
  void setDrumEnabled(bool enabled);
  void setDrumBPM(float bpm);
  void setKickEnabled(bool enabled);
  void setSnareEnabled(bool enabled);
  void setHiHatEnabled(bool enabled);
  void setHiHat16thNotes(bool is16th);

  // ===== DRUM PATTERN CONTROLS =====
  void setDrumStep(int instrument, int step, float velocity);
  float getDrumStep(int instrument, int step) const;
  void toggleDrumStep(int instrument, int step);
  void setDrumInstrumentVolume(int instrument, float volume);
  float getDrumInstrumentVolume(int instrument) const;
  void resetDrumPattern();

  // Sync drum machine to current loop position (call when enabling drums during
  // loop playback)
  void syncDrumToLoop();

  // ===== LOOPER CONTROLS =====
  void looperStartRecording();
  void looperStartRecordingTrack(int trackIndex);
  void looperStartPlayback();
  void looperStopPlayback();
  void looperClearLoop();
  void looperClearTrack(int trackIndex);
  void looperClearAllTracks();
  void looperCancelRecording();

  // Looper track controls
  void looperSetTrackVolume(int trackIndex, float volume);
  void looperSetTrackMuted(int trackIndex, bool muted);
  void looperSetTrackSolo(int trackIndex, bool solo);

  // Looper state queries
  int getLooperState() const;
  bool looperHasLoop() const;
  int getLooperCurrentBeat() const;
  int getLooperCurrentBar() const;

  // Looper track queries
  bool looperTrackHasContent(int trackIndex) const;
  float looperGetTrackVolume(int trackIndex) const;
  bool looperIsTrackMuted(int trackIndex) const;
  bool looperIsTrackSolo(int trackIndex) const;
  int looperGetActiveRecordingTrack() const;
  int looperGetUsedTrackCount() const;

  // Looper bar count
  void looperSetBarCount(int bars);
  int looperGetBarCount() const;

  // Looper audio export
  std::vector<float> looperGetMixedBuffer(int trackMask) const;
  int64_t looperGetBufferSize() const;

  // Oboe data callback
  oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream,
                                        void *audioData,
                                        int32_t numFrames) override;

  // Oboe error callback - handles device changes (Bluetooth connect/disconnect)
  void onErrorAfterClose(oboe::AudioStream *audioStream,
                         oboe::Result error) override;

private:
  std::shared_ptr<oboe::AudioStream> mStream;
  PolyphonyManager mPolyphonyManager;
  WurlitzerEngine mWurlitzerEngine;
  DrumMachine mDrumMachine;
  Looper mLooper;
  Metronome mMetronome;

  // Synth effects chain (applied after polyphony manager)
  Tremolo mSynthTremolo;
  Delay mSynthDelay;
  Reverb mSynthReverb;

  std::mutex mMutex;

  bool mWurlitzerMode = false;
  float mSynthVolume = 0.7f;
  float mMetronomeVolume = 0.3f; // Default louder than previous 0.1f equivalent
  bool mDrumEnabledByUser = false; // Track if user manually enabled drums
  std::atomic<bool> mIsRestarting{false}; // Prevent multiple restarts

  static constexpr int SAMPLE_RATE = 48000;
  static constexpr int CHANNEL_COUNT = 2; // Stereo

  oboe::Result createStream();
};

} // namespace synthio

#endif // SYNTHIO_AUDIO_ENGINE_H
