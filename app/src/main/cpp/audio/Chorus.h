#ifndef SYNTHIO_CHORUS_H
#define SYNTHIO_CHORUS_H

#define _USE_MATH_DEFINES
#include <cmath>
#include <vector>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

/**
 * Juno-106 style stereo chorus effect
 * Dual bucket brigade delay (BBD) emulation
 * Two modes: I (subtle) and II (deeper warble)
 */
class Chorus {
public:
    enum class Mode {
        OFF = 0,
        MODE_I = 1,   // Slow, subtle movement
        MODE_II = 2   // Faster, deeper warble
    };
    
    Chorus();
    
    void setSampleRate(float sampleRate);
    void setMode(Mode mode);
    Mode getMode() const { return mMode; }
    
    // Process mono input, returns stereo pair (left, right)
    void process(float input, float& outLeft, float& outRight);
    
    // Reset delay lines
    void reset();

private:
    float mSampleRate = 48000.0f;
    Mode mMode = Mode::OFF;
    
    // Delay line (circular buffer)
    std::vector<float> mDelayLine;
    int mDelayLineSize = 0;
    int mWriteIndex = 0;
    
    // Two LFOs for stereo modulation (inverted phase)
    float mLfoPhase = 0.0f;
    float mLfoRate = 0.5f;        // Hz
    float mLfoDepth = 0.002f;     // Delay time modulation depth in seconds
    float mBaseDelay = 0.007f;    // Base delay time in seconds (~7ms)
    
    // Mode parameters
    struct ModeParams {
        float rate;       // LFO rate in Hz
        float depth;      // Modulation depth in seconds
        float baseDelay;  // Base delay in seconds
        float wetMix;     // Wet signal level
    };
    
    static constexpr ModeParams MODE_I_PARAMS = {0.5f, 0.0015f, 0.006f, 0.5f};
    static constexpr ModeParams MODE_II_PARAMS = {0.8f, 0.003f, 0.008f, 0.6f};
    
    ModeParams mCurrentParams = MODE_I_PARAMS;
    
    // Read from delay line with linear interpolation
    float readDelayLine(float delaySamples);
    
    // Update parameters based on mode
    void updateModeParams();
};

} // namespace synthio

#endif // SYNTHIO_CHORUS_H
