#ifndef SYNTHIO_ENVELOPE_H
#define SYNTHIO_ENVELOPE_H

namespace synthio {

enum class EnvelopeStage {
    IDLE,
    ATTACK,
    DECAY,
    SUSTAIN,
    RELEASE
};

class Envelope {
public:
    Envelope();
    
    void setSampleRate(float sampleRate);
    
    // Set times in seconds
    void setAttack(float attackTime);
    void setDecay(float decayTime);
    void setSustain(float sustainLevel); // 0.0 to 1.0
    void setRelease(float releaseTime);
    
    void gate(bool isOn);
    float nextSample();
    
    bool isActive() const { return mStage != EnvelopeStage::IDLE; }
    EnvelopeStage getStage() const { return mStage; }

private:
    float mSampleRate = 48000.0f;
    
    float mAttackRate = 0.0f;
    float mDecayRate = 0.0f;
    float mSustainLevel = 0.7f;
    float mReleaseRate = 0.0f;
    
    float mAttackTime = 0.01f;
    float mDecayTime = 0.1f;
    float mReleaseTime = 0.3f;
    
    float mCurrentLevel = 0.0f;
    EnvelopeStage mStage = EnvelopeStage::IDLE;
    
    void calculateRates();
    float calculateRate(float time);
    
    // Exponential curve coefficient
    static constexpr float CURVE_COEFFICIENT = 0.0001f;
};

} // namespace synthio

#endif // SYNTHIO_ENVELOPE_H
