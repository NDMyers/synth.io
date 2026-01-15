cmake_minimum_required(VERSION 3.22.1)

project("synthio")

# Find the Oboe package
find_package(oboe REQUIRED CONFIG)

# Add our native library
add_library(synthio SHARED
    native-lib.cpp
    audio/AudioEngine.cpp
    audio/Oscillator.cpp
    audio/Envelope.cpp
    audio/Filter.cpp
    audio/Voice.cpp
    audio/PolyphonyManager.cpp
    audio/DrumSynth.cpp
    audio/DrumMachine.cpp
    audio/LFO.cpp
    audio/Chorus.cpp
    audio/WurlitzerVoice.cpp
    audio/WurlitzerEngine.cpp
    audio/Tremolo.cpp
    audio/Delay.cpp
    audio/Reverb.cpp
    audio/Looper.cpp
    audio/Metronome.cpp
)

# Set C++ standard
target_compile_features(synthio PRIVATE cxx_std_17)

# Link with Oboe and Android log
target_link_libraries(synthio
    oboe::oboe
    android
    log
)
