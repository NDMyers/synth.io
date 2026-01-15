#include <jni.h>
#include <memory>
#include "audio/AudioEngine.h"

static std::unique_ptr<synthio::AudioEngine> gAudioEngine;

extern "C" {

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeCreate(JNIEnv *env, jobject thiz) {
    gAudioEngine = std::make_unique<synthio::AudioEngine>();
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeDestroy(JNIEnv *env, jobject thiz) {
    gAudioEngine.reset();
}

JNIEXPORT jboolean JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeStart(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->start();
    }
    return false;
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeStop(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeNoteOn(JNIEnv *env, jobject thiz,
                                                          jint midiNote, jfloat frequency) {
    if (gAudioEngine) {
        gAudioEngine->noteOn(midiNote, frequency);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeNoteOff(JNIEnv *env, jobject thiz,
                                                           jint midiNote) {
    if (gAudioEngine) {
        gAudioEngine->noteOff(midiNote);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeAllNotesOff(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->allNotesOff();
    }
}

// ===== OSCILLATOR PARAMETERS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWaveform(JNIEnv *env, jobject thiz,
                                                               jint waveform) {
    if (gAudioEngine) {
        gAudioEngine->setWaveform(waveform);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetPulseWidth(JNIEnv *env, jobject thiz,
                                                                  jfloat width) {
    if (gAudioEngine) {
        gAudioEngine->setPulseWidth(width);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSubOscLevel(JNIEnv *env, jobject thiz,
                                                                   jfloat level) {
    if (gAudioEngine) {
        gAudioEngine->setSubOscLevel(level);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetNoiseLevel(JNIEnv *env, jobject thiz,
                                                                  jfloat level) {
    if (gAudioEngine) {
        gAudioEngine->setNoiseLevel(level);
    }
}

// ===== FILTER PARAMETERS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetFilterCutoff(JNIEnv *env, jobject thiz,
                                                                   jfloat cutoffHz) {
    if (gAudioEngine) {
        gAudioEngine->setFilterCutoff(cutoffHz);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetFilterResonance(JNIEnv *env, jobject thiz,
                                                                      jfloat resonance) {
    if (gAudioEngine) {
        gAudioEngine->setFilterResonance(resonance);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetFilterEnvAmount(JNIEnv *env, jobject thiz,
                                                                      jfloat amount) {
    if (gAudioEngine) {
        gAudioEngine->setFilterEnvelopeAmount(amount);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetFilterKeyTracking(JNIEnv *env, jobject thiz,
                                                                         jfloat amount) {
    if (gAudioEngine) {
        gAudioEngine->setFilterKeyTracking(amount);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetHPFCutoff(JNIEnv *env, jobject thiz,
                                                                 jfloat cutoffHz) {
    if (gAudioEngine) {
        gAudioEngine->setHPFCutoff(cutoffHz);
    }
}

// ===== ENVELOPE (ADSR) =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetAttack(JNIEnv *env, jobject thiz,
                                                             jfloat time) {
    if (gAudioEngine) {
        gAudioEngine->setAttack(time);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetDecay(JNIEnv *env, jobject thiz,
                                                            jfloat time) {
    if (gAudioEngine) {
        gAudioEngine->setDecay(time);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSustain(JNIEnv *env, jobject thiz,
                                                              jfloat level) {
    if (gAudioEngine) {
        gAudioEngine->setSustain(level);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetRelease(JNIEnv *env, jobject thiz,
                                                              jfloat time) {
    if (gAudioEngine) {
        gAudioEngine->setRelease(time);
    }
}

// ===== LFO PARAMETERS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetLFORate(JNIEnv *env, jobject thiz,
                                                               jfloat rateHz) {
    if (gAudioEngine) {
        gAudioEngine->setLFORate(rateHz);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetLFOPitchDepth(JNIEnv *env, jobject thiz,
                                                                     jfloat depth) {
    if (gAudioEngine) {
        gAudioEngine->setLFOPitchDepth(depth);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetLFOFilterDepth(JNIEnv *env, jobject thiz,
                                                                      jfloat depth) {
    if (gAudioEngine) {
        gAudioEngine->setLFOFilterDepth(depth);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetLFOPWMDepth(JNIEnv *env, jobject thiz,
                                                                   jfloat depth) {
    if (gAudioEngine) {
        gAudioEngine->setLFOPWMDepth(depth);
    }
}

// ===== CHORUS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetChorusMode(JNIEnv *env, jobject thiz,
                                                                  jint mode) {
    if (gAudioEngine) {
        gAudioEngine->setChorusMode(mode);
    }
}

// ===== SYNTH EFFECTS (Delay, Reverb, Tremolo) =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthTremoloRate(JNIEnv *env, jobject thiz,
                                                                        jfloat rate) {
    if (gAudioEngine) {
        gAudioEngine->setSynthTremoloRate(rate);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthTremoloDepth(JNIEnv *env, jobject thiz,
                                                                         jfloat depth) {
    if (gAudioEngine) {
        gAudioEngine->setSynthTremoloDepth(depth);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthReverbSize(JNIEnv *env, jobject thiz,
                                                                       jfloat size) {
    if (gAudioEngine) {
        gAudioEngine->setSynthReverbSize(size);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthReverbMix(JNIEnv *env, jobject thiz,
                                                                      jfloat mix) {
    if (gAudioEngine) {
        gAudioEngine->setSynthReverbMix(mix);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthDelayTime(JNIEnv *env, jobject thiz,
                                                                      jfloat time) {
    if (gAudioEngine) {
        gAudioEngine->setSynthDelayTime(time);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthDelayFeedback(JNIEnv *env, jobject thiz,
                                                                          jfloat feedback) {
    if (gAudioEngine) {
        gAudioEngine->setSynthDelayFeedback(feedback);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthDelayMix(JNIEnv *env, jobject thiz,
                                                                     jfloat mix) {
    if (gAudioEngine) {
        gAudioEngine->setSynthDelayMix(mix);
    }
}

// ===== GLIDE/PORTAMENTO =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetGlideTime(JNIEnv *env, jobject thiz,
                                                                 jfloat time) {
    if (gAudioEngine) {
        gAudioEngine->setGlideTime(time);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetGlideEnabled(JNIEnv *env, jobject thiz,
                                                                    jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setGlideEnabled(enabled);
    }
}

// ===== UNISON MODE =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetUnisonEnabled(JNIEnv *env, jobject thiz,
                                                                     jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setUnisonEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetUnisonVoices(JNIEnv *env, jobject thiz,
                                                                    jint count) {
    if (gAudioEngine) {
        gAudioEngine->setUnisonVoices(count);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetUnisonDetune(JNIEnv *env, jobject thiz,
                                                                    jfloat cents) {
    if (gAudioEngine) {
        gAudioEngine->setUnisonDetune(cents);
    }
}

// ===== VOLUME CONTROLS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSynthVolume(JNIEnv *env, jobject thiz,
                                                                   jfloat volume) {
    if (gAudioEngine) {
        gAudioEngine->setSynthVolume(volume);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetDrumVolume(JNIEnv *env, jobject thiz,
                                                                  jfloat volume) {
    if (gAudioEngine) {
        gAudioEngine->setDrumVolume(volume);
    }
}

// ===== DRUM MACHINE CONTROLS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetDrumEnabled(JNIEnv *env, jobject thiz,
                                                                   jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setDrumEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetDrumBPM(JNIEnv *env, jobject thiz,
                                                               jfloat bpm) {
    if (gAudioEngine) {
        gAudioEngine->setDrumBPM(bpm);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetKickEnabled(JNIEnv *env, jobject thiz,
                                                                   jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setKickEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetSnareEnabled(JNIEnv *env, jobject thiz,
                                                                    jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setSnareEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetHiHatEnabled(JNIEnv *env, jobject thiz,
                                                                    jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setHiHatEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetHiHat16thNotes(JNIEnv *env, jobject thiz,
                                                                      jboolean is16th) {
    if (gAudioEngine) {
        gAudioEngine->setHiHat16thNotes(is16th);
    }
}

// ===== WURLITZER CONTROLS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWurlitzerMode(JNIEnv *env, jobject thiz,
                                                                    jboolean enabled) {
    if (gAudioEngine) {
        gAudioEngine->setWurlitzerMode(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWurliTremolo(JNIEnv *env, jobject thiz,
                                                                   jfloat rate, jfloat depth) {
    if (gAudioEngine) {
        gAudioEngine->setWurliTremoloRate(rate);
        gAudioEngine->setWurliTremoloDepth(depth);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWurliChorusMode(JNIEnv *env, jobject thiz,
                                                                      jint mode) {
    if (gAudioEngine) {
        gAudioEngine->setWurliChorusMode(mode);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWurliReverb(JNIEnv *env, jobject thiz,
                                                                  jfloat size, jfloat mix) {
    if (gAudioEngine) {
        gAudioEngine->setWurliReverbSize(size);
        gAudioEngine->setWurliReverbMix(mix);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWurliDelay(JNIEnv *env, jobject thiz,
                                                                 jfloat time, jfloat feedback, jfloat mix) {
    if (gAudioEngine) {
        gAudioEngine->setWurliDelayTime(time);
        gAudioEngine->setWurliDelayFeedback(feedback);
        gAudioEngine->setWurliDelayMix(mix);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeSetWurliVolume(JNIEnv *env, jobject thiz,
                                                                  jfloat volume) {
    if (gAudioEngine) {
        gAudioEngine->setWurliVolume(volume);
    }
}

// ===== LOOPER CONTROLS =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperStartRecording(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->looperStartRecording();
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperStartPlayback(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->looperStartPlayback();
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperStopPlayback(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->looperStopPlayback();
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperClearLoop(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->looperClearLoop();
    }
}

JNIEXPORT jint JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeGetLooperState(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->getLooperState();
    }
    return 0;  // IDLE
}

JNIEXPORT jboolean JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperHasLoop(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->looperHasLoop();
    }
    return false;
}

JNIEXPORT jint JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeGetLooperCurrentBeat(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->getLooperCurrentBeat();
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeGetLooperCurrentBar(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->getLooperCurrentBar();
    }
    return 0;
}

// ===== MULTI-TRACK LOOPER =====

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperStartRecordingTrack(JNIEnv *env, jobject thiz,
                                                                              jint trackIndex) {
    if (gAudioEngine) {
        gAudioEngine->looperStartRecordingTrack(trackIndex);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperClearTrack(JNIEnv *env, jobject thiz,
                                                                     jint trackIndex) {
    if (gAudioEngine) {
        gAudioEngine->looperClearTrack(trackIndex);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperClearAllTracks(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        gAudioEngine->looperClearAllTracks();
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperSetTrackVolume(JNIEnv *env, jobject thiz,
                                                                         jint trackIndex, jfloat volume) {
    if (gAudioEngine) {
        gAudioEngine->looperSetTrackVolume(trackIndex, volume);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperSetTrackMuted(JNIEnv *env, jobject thiz,
                                                                        jint trackIndex, jboolean muted) {
    if (gAudioEngine) {
        gAudioEngine->looperSetTrackMuted(trackIndex, muted);
    }
}

JNIEXPORT void JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperSetTrackSolo(JNIEnv *env, jobject thiz,
                                                                       jint trackIndex, jboolean solo) {
    if (gAudioEngine) {
        gAudioEngine->looperSetTrackSolo(trackIndex, solo);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperTrackHasContent(JNIEnv *env, jobject thiz,
                                                                          jint trackIndex) {
    if (gAudioEngine) {
        return gAudioEngine->looperTrackHasContent(trackIndex);
    }
    return false;
}

JNIEXPORT jfloat JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperGetTrackVolume(JNIEnv *env, jobject thiz,
                                                                         jint trackIndex) {
    if (gAudioEngine) {
        return gAudioEngine->looperGetTrackVolume(trackIndex);
    }
    return 0.7f;
}

JNIEXPORT jboolean JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperIsTrackMuted(JNIEnv *env, jobject thiz,
                                                                       jint trackIndex) {
    if (gAudioEngine) {
        return gAudioEngine->looperIsTrackMuted(trackIndex);
    }
    return false;
}

JNIEXPORT jboolean JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperIsTrackSolo(JNIEnv *env, jobject thiz,
                                                                      jint trackIndex) {
    if (gAudioEngine) {
        return gAudioEngine->looperIsTrackSolo(trackIndex);
    }
    return false;
}

JNIEXPORT jint JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperGetActiveRecordingTrack(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->looperGetActiveRecordingTrack();
    }
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_synthio_app_audio_SynthesizerEngine_nativeLooperGetUsedTrackCount(JNIEnv *env, jobject thiz) {
    if (gAudioEngine) {
        return gAudioEngine->looperGetUsedTrackCount();
    }
    return 0;
}

} // extern "C"
