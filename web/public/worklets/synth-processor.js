/**
 * Synth.io AudioWorklet Processor
 * Polyphonic synthesizer with ADSR envelope running on audio thread
 * 
 * Note: AudioWorklet processors must be pure JavaScript (no TypeScript)
 */

class SynthProcessor extends AudioWorkletProcessor {
    constructor(options) {
        super();

        const processorOptions = options.processorOptions || {};

        this.maxVoices = 16;
        this.lfoPhase = 0;

        this.params = {
            sampleRate: processorOptions.sampleRate || 48000,
            waveform: 'sawtooth',
            attack: 0.01,
            decay: 0.1,
            sustain: 0.7,
            release: 0.3,
            filterCutoff: 8000,
            filterResonance: 1,
            lfoRate: 4,
            lfoAmount: 0,
        };

        // Initialize voice pool
        this.voices = [];
        for (let i = 0; i < this.maxVoices; i++) {
            this.voices.push({
                active: false,
                midiNote: 0,
                frequency: 440,
                phase: 0,
                envelope: { stage: 'off', level: 0, startTime: 0 },
            });
        }

        // Handle messages from main thread
        this.port.onmessage = (event) => {
            const { type, data } = event.data;

            switch (type) {
                case 'noteOn':
                    this.handleNoteOn(data.note, data.velocity);
                    break;
                case 'noteOff':
                    this.handleNoteOff(data.note);
                    break;
                case 'setParam':
                    this.setParameter(data.param, data.value);
                    break;
                case 'allNotesOff':
                    this.allNotesOff();
                    break;
            }
        };
    }

    midiToFrequency(midiNote) {
        return 440 * Math.pow(2, (midiNote - 69) / 12);
    }

    handleNoteOn(midiNote, velocity = 1) {
        // Find existing voice for this note or get a free voice
        let voice = this.voices.find(v => v.midiNote === midiNote && v.active);

        if (!voice) {
            voice = this.voices.find(v => !v.active);
        }

        if (!voice) {
            // Steal oldest voice
            voice = this.voices[0];
        }

        voice.active = true;
        voice.midiNote = midiNote;
        voice.frequency = this.midiToFrequency(midiNote);
        voice.phase = 0;
        voice.envelope = {
            stage: 'attack',
            level: 0,
            startTime: currentTime,
        };
    }

    handleNoteOff(midiNote) {
        const voice = this.voices.find(v => v.midiNote === midiNote && v.active);
        if (voice && voice.envelope.stage !== 'release') {
            voice.envelope.stage = 'release';
            voice.envelope.startTime = currentTime;
        }
    }

    allNotesOff() {
        this.voices.forEach(v => {
            if (v.active) {
                v.envelope.stage = 'release';
                v.envelope.startTime = currentTime;
            }
        });
    }

    setParameter(param, value) {
        if (param in this.params) {
            this.params[param] = value;
        }
    }

    generateWaveform(phase, waveform) {
        const p = phase % 1;

        switch (waveform) {
            case 'sine':
                return Math.sin(2 * Math.PI * p);
            case 'square':
                return p < 0.5 ? 1 : -1;
            case 'sawtooth':
                return 2 * p - 1;
            case 'triangle':
                return 4 * Math.abs(p - 0.5) - 1;
            default:
                return Math.sin(2 * Math.PI * p);
        }
    }

    processEnvelope(voice, deltaTime) {
        const env = voice.envelope;
        const { attack, decay, sustain, release } = this.params;

        switch (env.stage) {
            case 'attack':
                env.level += deltaTime / Math.max(attack, 0.001);
                if (env.level >= 1) {
                    env.level = 1;
                    env.stage = 'decay';
                }
                break;
            case 'decay':
                env.level -= deltaTime / Math.max(decay, 0.001) * (1 - sustain);
                if (env.level <= sustain) {
                    env.level = sustain;
                    env.stage = 'sustain';
                }
                break;
            case 'sustain':
                env.level = sustain;
                break;
            case 'release':
                env.level -= deltaTime / Math.max(release, 0.001);
                if (env.level <= 0) {
                    env.level = 0;
                    env.stage = 'off';
                    voice.active = false;
                }
                break;
            case 'off':
                env.level = 0;
                break;
        }

        return Math.max(0, Math.min(1, env.level));
    }

    process(inputs, outputs, parameters) {
        const output = outputs[0];
        const left = output[0];
        const right = output[1];

        if (!left || !right) return true;

        const bufferSize = left.length;
        const sampleRate = this.params.sampleRate;
        const deltaTime = 1 / sampleRate;

        // Process each sample
        for (let i = 0; i < bufferSize; i++) {
            let sampleL = 0;
            let sampleR = 0;

            // LFO (shared across voices)
            this.lfoPhase += this.params.lfoRate / sampleRate;
            if (this.lfoPhase >= 1) this.lfoPhase -= 1;
            const lfo = Math.sin(2 * Math.PI * this.lfoPhase) * this.params.lfoAmount;

            // Process each active voice
            for (const voice of this.voices) {
                if (!voice.active && voice.envelope.stage === 'off') continue;

                // Apply LFO to frequency
                const freqMod = voice.frequency * (1 + lfo * 0.1);

                // Generate waveform
                const sample = this.generateWaveform(voice.phase, this.params.waveform);

                // Apply envelope
                const envLevel = this.processEnvelope(voice, deltaTime);

                // Accumulate (with simple stereo spread)
                const voiceSample = sample * envLevel * 0.25; // Gain reduction for polyphony
                sampleL += voiceSample;
                sampleR += voiceSample;

                // Advance phase
                voice.phase += freqMod / sampleRate;
                if (voice.phase >= 1) voice.phase -= 1;
            }

            // Soft clipping
            left[i] = Math.tanh(sampleL);
            right[i] = Math.tanh(sampleR);
        }

        return true;
    }
}

registerProcessor('synth-processor', SynthProcessor);
