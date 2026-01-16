#ifndef SYNTHIO_FILTER_H
#define SYNTHIO_FILTER_H

namespace synthio {

/**
 * Enhanced filter with Juno-106 style characteristics
 * - Resonant low-pass filter with self-oscillation capability
 * - Non-resonant high-pass filter (HPF)
 * - Bass boost mode (when HPF is at 0)
 */
class Filter {
public:
    Filter();
    
    void setSampleRate(float sampleRate);
    
    // Low-pass filter
    void setCutoff(float cutoffHz);
    void setResonance(float resonance);  // 0.0 to 1.0 (1.0 = self-oscillation)
    
    // High-pass filter (0 = bass boost, higher = more bass cut)
    void setHPFCutoff(float cutoffHz);   // 0 to 1000 Hz
    
    // Key tracking: filter follows pitch (0.0 = off, 1.0 = full tracking)
    void setKeyTracking(float amount);
    void setNoteFrequency(float freq);   // Current note frequency for key tracking
    
    float process(float input);
    void reset();
    
private:
    float mSampleRate = 48000.0f;
    
    // LPF parameters
    float mCutoff = 10000.0f;
    float mResonance = 0.0f;
    float mTargetCutoff = 10000.0f;
    float mSmoothingFactor = 0.001f;
    
    // LPF Biquad coefficients
    float mA0 = 1.0f, mA1 = 0.0f, mA2 = 0.0f;
    float mB1 = 0.0f, mB2 = 0.0f;
    
    // LPF state
    float mX1 = 0.0f, mX2 = 0.0f;
    float mY1 = 0.0f, mY2 = 0.0f;
    
    // HPF parameters (simple one-pole)
    float mHPFCutoff = 0.0f;
    float mHPFCoeff = 0.0f;
    float mHPFState = 0.0f;
    
    // Bass boost (when HPF = 0)
    float mBassBoostAmount = 1.2f;  // Subtle bass boost
    
    // Key tracking
    float mKeyTracking = 0.0f;
    float mNoteFrequency = 440.0f;
    
    // DC blocker state for bass boost mode
    float mDCBlockState = 0.0f;
    
    void calculateLPFCoefficients();
    void calculateHPFCoefficient();
    
    // Soft saturation to prevent clipping at high resonance
    float softSaturate(float x);
};

} // namespace synthio

#endif // SYNTHIO_FILTER_H
