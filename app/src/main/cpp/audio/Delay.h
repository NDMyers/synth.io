#ifndef SYNTHIO_DELAY_H
#define SYNTHIO_DELAY_H

#include <vector>

namespace synthio {

/**
 * Stereo delay effect with feedback and filtering
 * Suitable for Wurlitzer-style warm delay effects
 */
class Delay {
public:
    Delay();
    
    void setSampleRate(float sampleRate);
    
    // Delay time in seconds (0.05 - 0.5)
    void setTime(float timeSeconds);
    
    // Feedback 0.0 - 0.8
    void setFeedback(float feedback);
    
    // Wet/dry mix 0.0 - 1.0
    void setMix(float mix);
    
    // Process stereo
    void process(float& left, float& right);

private:
    float mSampleRate = 48000.0f;
    float mTime = 0.25f;
    float mFeedback = 0.3f;
    float mMix = 0.3f;
    
    // Delay buffers
    std::vector<float> mBufferL;
    std::vector<float> mBufferR;
    int mWritePos = 0;
    int mDelaySamples = 0;
    int mMaxDelaySamples = 0;
    
    // Low-pass filter in feedback path for warmth
    float mFilterStateL = 0.0f;
    float mFilterStateR = 0.0f;
    float mFilterCoeff = 0.3f;  // ~3kHz cutoff
    
    void updateDelaySamples();
};

} // namespace synthio

#endif // SYNTHIO_DELAY_H
