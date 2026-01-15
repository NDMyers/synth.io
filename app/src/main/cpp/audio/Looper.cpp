#include "Looper.h"
#include <algorithm>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "SynthIO_Looper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace synthio {

Looper::Looper() {
    updateTiming();
}

void Looper::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
    updateTiming();
}

void Looper::setBPM(float bpm) {
    mBPM = std::max(30.0f, std::min(300.0f, bpm));
    updateTiming();
}

void Looper::updateTiming() {
    // Calculate samples per beat: (60 / BPM) * sampleRate
    float secondsPerBeat = 60.0f / mBPM;
    mSamplesPerBeat = static_cast<int>(secondsPerBeat * mSampleRate);
    mSamplesPerBar = mSamplesPerBeat * BEATS_PER_BAR;
    
    // Only update loop length if not locked (first recording sets the length)
    if (!mLoopLengthLocked) {
        mLoopLengthSamples = mSamplesPerBar * BARS_TO_RECORD;
    }
}

// ===== MAIN CONTROL =====

void Looper::startRecording() {
    // Backward compatibility: start recording track 0
    startRecordingTrack(0);
}

void Looper::startRecordingTrack(int trackIndex) {
    if (!isValidTrackIndex(trackIndex)) {
        LOGI("Invalid track index: %d", trackIndex);
        return;
    }
    
    if (mTracks[trackIndex].hasContent) {
        LOGI("Track %d already has content, clear it first", trackIndex);
        return;
    }
    
    if (mState == State::RECORDING || mState == State::PRE_COUNT) {
        LOGI("Already recording, cannot start another track");
        return;
    }
    
    // If currently playing, we'll continue playback of other tracks while recording
    // If stopped, we'll start playback when recording begins
    
    // Update timing before recording (uses current BPM)
    // But if we already have a loop, keep the locked length
    if (!mLoopLengthLocked) {
        updateTiming();
    }
    
    // Allocate buffer for this track
    mTracks[trackIndex].bufferL.resize(mLoopLengthSamples, 0.0f);
    mTracks[trackIndex].bufferR.resize(mLoopLengthSamples, 0.0f);
    std::fill(mTracks[trackIndex].bufferL.begin(), mTracks[trackIndex].bufferL.end(), 0.0f);
    std::fill(mTracks[trackIndex].bufferR.begin(), mTracks[trackIndex].bufferR.end(), 0.0f);
    
    mActiveRecordingTrack = trackIndex;
    mState = State::PRE_COUNT;
    mPreCountPosition = 0;
    mRecordPosition = 0;
    mCurrentBeat = 0;
    mCurrentBar = 0;
    
    LOGI("Starting pre-count for track %d, loop length: %lld samples", trackIndex, mLoopLengthSamples);
    notifyStateChange();
}

void Looper::stopPlayback() {
    if (mState == State::PLAYING) {
        mState = State::STOPPED;
        mPlaybackPosition = 0;
        LOGI("Playback stopped");
        notifyStateChange();
    }
}

void Looper::startPlayback() {
    if (hasAnyLoop() && mState == State::STOPPED) {
        mState = State::PLAYING;
        mPlaybackPosition = 0;
        mCurrentBeat = 0;
        mCurrentBar = 0;
        LOGI("Playback started");
        notifyStateChange();
    }
}

void Looper::clearLoop() {
    // Backward compatibility: clear all tracks
    clearAllTracks();
}

// ===== TRACK CONTROLS =====

void Looper::setTrackVolume(int trackIndex, float volume) {
    if (isValidTrackIndex(trackIndex)) {
        mTracks[trackIndex].volume = std::max(0.0f, std::min(1.0f, volume));
    }
}

void Looper::setTrackMuted(int trackIndex, bool muted) {
    if (isValidTrackIndex(trackIndex)) {
        mTracks[trackIndex].muted = muted;
    }
}

void Looper::setTrackSolo(int trackIndex, bool solo) {
    if (isValidTrackIndex(trackIndex)) {
        mTracks[trackIndex].solo = solo;
    }
}

void Looper::clearTrack(int trackIndex) {
    if (!isValidTrackIndex(trackIndex)) return;
    
    // Can't clear while recording this track
    if (mActiveRecordingTrack == trackIndex && 
        (mState == State::PRE_COUNT || mState == State::RECORDING)) {
        return;
    }
    
    mTracks[trackIndex].bufferL.clear();
    mTracks[trackIndex].bufferR.clear();
    mTracks[trackIndex].hasContent = false;
    mTracks[trackIndex].volume = 0.7f;
    mTracks[trackIndex].muted = false;
    mTracks[trackIndex].solo = false;
    
    LOGI("Track %d cleared", trackIndex);
    
    // If no tracks have content anymore, reset state
    if (!hasAnyLoop()) {
        mState = State::IDLE;
        mLoopLengthLocked = false;
        mPlaybackPosition = 0;
        notifyStateChange();
    }
}

void Looper::clearAllTracks() {
    if (mState == State::PLAYING) {
        stopPlayback();
    }
    
    for (int i = 0; i < MAX_TRACKS; i++) {
        mTracks[i].bufferL.clear();
        mTracks[i].bufferR.clear();
        mTracks[i].hasContent = false;
        mTracks[i].volume = 0.7f;
        mTracks[i].muted = false;
        mTracks[i].solo = false;
    }
    
    mState = State::IDLE;
    mActiveRecordingTrack = -1;
    mLoopLengthLocked = false;
    mPlaybackPosition = 0;
    mRecordPosition = 0;
    mCurrentBeat = 0;
    mCurrentBar = 0;
    
    LOGI("All tracks cleared");
    notifyStateChange();
}

// ===== STATE/TRACK QUERIES =====

bool Looper::hasAnyLoop() const {
    for (int i = 0; i < MAX_TRACKS; i++) {
        if (mTracks[i].hasContent) return true;
    }
    return false;
}

bool Looper::trackHasContent(int trackIndex) const {
    if (!isValidTrackIndex(trackIndex)) return false;
    return mTracks[trackIndex].hasContent;
}

float Looper::getTrackVolume(int trackIndex) const {
    if (!isValidTrackIndex(trackIndex)) return 0.0f;
    return mTracks[trackIndex].volume;
}

bool Looper::isTrackMuted(int trackIndex) const {
    if (!isValidTrackIndex(trackIndex)) return false;
    return mTracks[trackIndex].muted;
}

bool Looper::isTrackSolo(int trackIndex) const {
    if (!isValidTrackIndex(trackIndex)) return false;
    return mTracks[trackIndex].solo;
}

int Looper::getUsedTrackCount() const {
    int count = 0;
    for (int i = 0; i < MAX_TRACKS; i++) {
        if (mTracks[i].hasContent) count++;
    }
    return count;
}

bool Looper::anySolo() const {
    for (int i = 0; i < MAX_TRACKS; i++) {
        if (mTracks[i].hasContent && mTracks[i].solo) return true;
    }
    return false;
}

// ===== AUDIO PROCESSING =====

void Looper::process(float synthL, float synthR, float& loopOutL, float& loopOutR) {
    loopOutL = 0.0f;
    loopOutR = 0.0f;
    
    switch (mState) {
        case State::PRE_COUNT: {
            // During pre-count, play existing tracks but don't record yet
            if (hasAnyLoop()) {
                bool hasSolo = anySolo();
                
                for (int i = 0; i < MAX_TRACKS; i++) {
                    if (!mTracks[i].hasContent) continue;
                    if (mTracks[i].muted) continue;
                    if (hasSolo && !mTracks[i].solo) continue;
                    
                    // Use mPlaybackPosition for playback during pre-count
                    if (mPlaybackPosition < static_cast<int64_t>(mTracks[i].bufferL.size())) {
                        loopOutL += mTracks[i].bufferL[mPlaybackPosition] * mTracks[i].volume;
                        loopOutR += mTracks[i].bufferR[mPlaybackPosition] * mTracks[i].volume;
                    }
                }
                
                // Advance playback position for existing tracks
                mPlaybackPosition++;
                if (mLoopLengthSamples > 0 && mPlaybackPosition >= mLoopLengthSamples) {
                    mPlaybackPosition = 0;
                }
            }
            
            // Track pre-count progress
            mPreCountPosition++;
            int beatInPreCount = static_cast<int>(mPreCountPosition / mSamplesPerBeat);
            if (beatInPreCount != mCurrentBeat) {
                mCurrentBeat = beatInPreCount;
                notifyStateChange();
            }
            
            // Check if pre-count is complete
            if (mPreCountPosition >= mSamplesPerBeat * PRE_COUNT_BEATS) {
                mState = State::RECORDING;
                mRecordPosition = 0;
                mCurrentBeat = 0;
                mCurrentBar = 0;
                
                // Sync playback position to start of loop
                mPlaybackPosition = 0;
                
                LOGI("Pre-count complete, starting recording on track %d", mActiveRecordingTrack);
                notifyStateChange();
            }
            break;
        }
        
        case State::RECORDING: {
            // Record synth audio to active track
            if (isValidTrackIndex(mActiveRecordingTrack) && 
                mRecordPosition < mLoopLengthSamples) {
                mTracks[mActiveRecordingTrack].bufferL[mRecordPosition] = synthL;
                mTracks[mActiveRecordingTrack].bufferR[mRecordPosition] = synthR;
            }
            
            // Also play back other tracks while recording
            bool hasSolo = anySolo();
            for (int i = 0; i < MAX_TRACKS; i++) {
                if (i == mActiveRecordingTrack) continue;  // Don't play the track we're recording
                if (!mTracks[i].hasContent) continue;
                if (mTracks[i].muted) continue;
                if (hasSolo && !mTracks[i].solo) continue;
                
                if (mRecordPosition < static_cast<int64_t>(mTracks[i].bufferL.size())) {
                    loopOutL += mTracks[i].bufferL[mRecordPosition] * mTracks[i].volume;
                    loopOutR += mTracks[i].bufferR[mRecordPosition] * mTracks[i].volume;
                }
            }
            
            mRecordPosition++;
            updateBeatBar();
            
            // Check if recording is complete
            if (mRecordPosition >= mLoopLengthSamples) {
                mTracks[mActiveRecordingTrack].hasContent = true;
                mLoopLengthLocked = true;  // Lock loop length after first recording
                mState = State::STOPPED;
                mActiveRecordingTrack = -1;
                mPlaybackPosition = 0;
                mCurrentBeat = 0;
                mCurrentBar = 0;
                
                LOGI("Recording complete, track now has content");
                notifyStateChange();
            }
            break;
        }
        
        case State::PLAYING: {
            // Mix all non-muted tracks (respecting solo)
            bool hasSolo = anySolo();
            
            for (int i = 0; i < MAX_TRACKS; i++) {
                if (!mTracks[i].hasContent) continue;
                if (mTracks[i].muted) continue;
                if (hasSolo && !mTracks[i].solo) continue;
                
                if (mPlaybackPosition < static_cast<int64_t>(mTracks[i].bufferL.size())) {
                    loopOutL += mTracks[i].bufferL[mPlaybackPosition] * mTracks[i].volume;
                    loopOutR += mTracks[i].bufferR[mPlaybackPosition] * mTracks[i].volume;
                }
            }
            
            mPlaybackPosition++;
            updateBeatBar();
            
            // Loop back to start
            if (mPlaybackPosition >= mLoopLengthSamples) {
                mPlaybackPosition = 0;
                mCurrentBeat = 0;
                mCurrentBar = 0;
            }
            break;
        }
        
        case State::IDLE:
        case State::STOPPED:
        default:
            // Nothing to do
            break;
    }
}

void Looper::updateBeatBar() {
    int64_t position = (mState == State::RECORDING) ? mRecordPosition : mPlaybackPosition;
    
    if (mSamplesPerBeat <= 0) return;
    
    int totalBeats = static_cast<int>(position / mSamplesPerBeat);
    int newBar = totalBeats / BEATS_PER_BAR;
    int newBeat = totalBeats % BEATS_PER_BAR;
    
    if (newBeat != mCurrentBeat || newBar != mCurrentBar) {
        mCurrentBeat = newBeat;
        mCurrentBar = newBar;
    }
}

void Looper::notifyStateChange() {
    if (mStateCallback) {
        mStateCallback(mState, mCurrentBeat);
    }
}

} // namespace synthio
