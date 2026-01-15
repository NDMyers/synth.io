#ifndef SYNTHIO_LFO_H
#define SYNTHIO_LFO_H

#define _USE_MATH_DEFINES
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace synthio {

/**
 * Low-Frequency Oscillator for modulation
 * Juno-106 style global LFO with triangle wave
 * Range: 0.1Hz to 20Hz
 */
class LFO {
public:
    LFO();
    
    void setSampleRate(float sampleRate);
    void setRate(float rateHz);  // 0.1 to 20 Hz
    
    // Modulation depths (0.0 to 1.0)
    void setPitchDepth(float depth);      // Vibrato
    void setFilterDepth(float depth);     // Filter wah
    void setPWMDepth(float depth);        // Pulse width modulation
    
    // Get current modulation values
    float getPitchMod() const;    // Returns pitch deviation in semitones
    float getFilterMod() const;   // Returns -1.0 to 1.0 for filter cutoff mod
    float getPWMMod() const;      // Returns -0.4 to 0.4 for pulse width mod
    
    // Process one sample (advances LFO phase)
    void tick();
    
    // Reset phase
    void reset();
    
    // Get raw LFO value (-1.0 to 1.0)
    float getValue() const { return mCurrentValue; }

private:
    float mSampleRate = 48000.0f;
    float mRate = 1.0f;           // Hz
    float mPhase = 0.0f;
    float mPhaseIncrement = 0.0f;
    float mCurrentValue = 0.0f;
    
    // Modulation depths
    float mPitchDepth = 0.0f;     // 0-1 maps to 0-2 semitones
    float mFilterDepth = 0.0f;    // 0-1 maps to filter mod amount
    float mPWMDepth = 0.0f;       // 0-1 maps to PWM range
    
    void updatePhaseIncrement();
    float generateTriangle();
};

} // namespace synthio

#endif // SYNTHIO_LFO_H
