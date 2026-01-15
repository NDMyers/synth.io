#ifndef SYNTHIO_TREMOLO_H
#define SYNTHIO_TREMOLO_H

namespace synthio {

/**
 * LDR-style Tremolo effect emulating Wurlitzer 200A tremolo circuit
 * 
 * The 200A uses an LED/LDR (vactrol) circuit that produces
 * a smooth, slightly asymmetric amplitude modulation.
 */
class Tremolo {
public:
    Tremolo();
    
    void setSampleRate(float sampleRate);
    
    // Rate in Hz (0.5 - 10.0, typical Wurlitzer ~5Hz)
    void setRate(float rateHz);
    
    // Depth 0.0 - 1.0
    void setDepth(float depth);
    
    // Process stereo sample
    void process(float& left, float& right);
    
    // Process mono sample
    float process(float input);

private:
    float mSampleRate = 48000.0f;
    float mRate = 5.0f;
    float mDepth = 0.5f;
    
    float mPhase = 0.0f;
    float mPhaseIncrement = 0.0f;
    
    // Smoothed modulation for LDR-like response
    float mCurrentMod = 1.0f;
    float mSmoothingCoeff = 0.999f;
    
    void updatePhaseIncrement();
};

} // namespace synthio

#endif // SYNTHIO_TREMOLO_H
