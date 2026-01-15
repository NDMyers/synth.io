#ifndef SYNTHIO_REVERB_H
#define SYNTHIO_REVERB_H

#include <vector>
#include <array>

namespace synthio {

/**
 * Simple Schroeder-style reverb suitable for Wurlitzer
 * Uses 4 parallel comb filters + 2 series allpass filters
 */
class Reverb {
public:
    Reverb();
    
    void setSampleRate(float sampleRate);
    
    // Room size 0.0 - 1.0
    void setSize(float size);
    
    // Damping (high frequency decay) 0.0 - 1.0
    void setDamping(float damping);
    
    // Wet/dry mix 0.0 - 1.0
    void setMix(float mix);
    
    // Process stereo
    void process(float& left, float& right);
    
    // Clear buffers
    void reset();

private:
    float mSampleRate = 48000.0f;
    float mSize = 0.5f;
    float mDamping = 0.5f;
    float mMix = 0.3f;
    
    // Comb filter delays (in samples at 48kHz, scaled for other rates)
    static constexpr int NUM_COMBS = 4;
    static constexpr int COMB_DELAYS[NUM_COMBS] = {1557, 1617, 1491, 1422};
    
    // Allpass filter delays
    static constexpr int NUM_ALLPASS = 2;
    static constexpr int ALLPASS_DELAYS[NUM_ALLPASS] = {225, 556};
    
    struct CombFilter {
        std::vector<float> buffer;
        int writePos = 0;
        float filterState = 0.0f;
        float feedback = 0.7f;
        float damping = 0.5f;
    };
    
    struct AllpassFilter {
        std::vector<float> buffer;
        int writePos = 0;
        float feedback = 0.5f;
    };
    
    std::array<CombFilter, NUM_COMBS> mCombsL;
    std::array<CombFilter, NUM_COMBS> mCombsR;
    std::array<AllpassFilter, NUM_ALLPASS> mAllpassL;
    std::array<AllpassFilter, NUM_ALLPASS> mAllpassR;
    
    void initializeFilters();
    float processComb(CombFilter& comb, float input, int delaySamples);
    float processAllpass(AllpassFilter& ap, float input, int delaySamples);
};

} // namespace synthio

#endif // SYNTHIO_REVERB_H
