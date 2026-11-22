#include "cosmium_audio.h"

#include <algorithm>
#include <cmath>

namespace cosmium::audio {
namespace {

constexpr double kPi = 3.14159265358979323846;

} // namespace

std::vector<int16_t> synthesizeWoodyTock(
    int sampleRate,
    double durationSec,
    double baseFreq,
    double sweep,
    double volume
) {
    int numSamples = std::max(1, static_cast<int>(sampleRate * durationSec));
    std::vector<int16_t> buf(numSamples);
    for (int i = 0; i < numSamples; ++i) {
        double t = static_cast<double>(i) / sampleRate;
        double freq = baseFreq + sweep * std::exp(-55.0 * t);
        double attack = std::clamp(t / 0.004, 0.0, 1.0);
        double decay = std::exp(-42.0 * t);
        double body = std::sin(2.0 * kPi * freq * t) * 0.8 +
                      std::sin(2.0 * kPi * (freq * 1.48) * t) * 0.2;
        double sample = std::clamp(body * attack * decay * volume, -0.9, 0.9);
        buf[i] = static_cast<int16_t>(sample * 32767.0);
    }
    return buf;
}

std::vector<int16_t> synthesizeTempleChime(
    int sampleRate,
    double durationSec,
    double fundamental
) {
    int numSamples = std::max(1, static_cast<int>(sampleRate * durationSec));
    std::vector<int16_t> buf(numSamples);
    for (int i = 0; i < numSamples; ++i) {
        double t = static_cast<double>(i) / sampleRate;
        double attack = std::clamp(t / 0.008, 0.0, 1.0);
        double envAmp = attack * std::exp(-14.5 * t);
        double overtoneEnv = attack * std::exp(-22.0 * t);
        double wave = std::sin(2.0 * kPi * fundamental * t) * 0.65 * envAmp +
                      std::sin(2.0 * kPi * (fundamental * 2.0) * t) * 0.22 * overtoneEnv +
                      std::sin(2.0 * kPi * (fundamental * 2.76) * t) * 0.13 * overtoneEnv;
        double sample = std::clamp(wave * 0.26, -0.9, 0.9);
        buf[i] = static_cast<int16_t>(sample * 32767.0);
    }
    return buf;
}

std::vector<int16_t> synthesizeVictorySwell(
    int sampleRate,
    double durationSec
) {
    int numSamples = std::max(1, static_cast<int>(sampleRate * durationSec));
    std::vector<int16_t> buf(numSamples);
    static constexpr double kFreqs[4] = {261.63, 392.00, 523.25, 659.25};
    static constexpr double kEntries[4] = {0.0, 0.06, 0.13, 0.21};

    for (int i = 0; i < numSamples; ++i) {
        double t = static_cast<double>(i) / sampleRate;
        double mix = 0.0;
        for (int k = 0; k < 4; ++k) {
            double dt = t - kEntries[k];
            if (dt > 0.0) {
                double envAmp = std::clamp(dt / 0.028, 0.0, 1.0) * std::exp(-3.8 * dt);
                double f = kFreqs[k];
                double voice = std::sin(2.0 * kPi * f * dt) * 0.58 +
                               std::sin(2.0 * kPi * (f * 2.0) * dt) * 0.26 +
                               std::sin(2.0 * kPi * (f * 3.0) * dt) * 0.16;
                mix += voice * envAmp;
            }
        }
        double sample = std::clamp(mix * 0.22, -0.92, 0.92);
        buf[i] = static_cast<int16_t>(sample * 32767.0);
    }
    return buf;
}

void synthesizeMusicNote(
    int16_t* outBuffer,
    int sampleCount,
    int sampleRate,
    double melodyFreq,
    double harmonicFreq,
    double droneFreq,
    bool isJoyful,
    int64_t globalSampleStart
) {
    if (!outBuffer || sampleCount <= 0) return;
    const double decayRate = isJoyful ? 5.5 : 2.2;
    const double harmonicWeight = isJoyful ? 0.18 : 0.12;
    const double droneEnv = isJoyful ? 0.25 : 0.42;

    for (int i = 0; i < sampleCount; ++i) {
        double tLocal = static_cast<double>(i) / sampleRate;
        double tGlobal = static_cast<double>(globalSampleStart + i) / sampleRate;
        double attack = std::clamp(tLocal / 0.025, 0.0, 1.0);
        double envAmp = attack * std::exp(-decayRate * tLocal);
        double melodyWave = std::sin(2.0 * kPi * melodyFreq * tLocal) * 0.65 +
                            std::sin(2.0 * kPi * harmonicFreq * tLocal) * harmonicWeight;
        double droneWave = std::sin(2.0 * kPi * droneFreq * tGlobal) * droneEnv +
                           std::sin(2.0 * kPi * (droneFreq * 1.5) * tGlobal) * (droneEnv * 0.45);
        double mixed = std::clamp((melodyWave * envAmp + droneWave) * 0.14, -0.95, 0.95);
        outBuffer[i] = static_cast<int16_t>(mixed * 32767.0);
    }
}

std::vector<int16_t> synthesizeGameMusicStep(
    int gameTypeOrdinal,
    int noteIndex,
    int sampleRate,
    int64_t globalSampleStart
) {
    static constexpr double kLudoPattern[12] = {
        523.25, 659.25, 783.99, 880.00, 783.99, 659.25,
        587.33, 523.25, 698.46, 880.00, 1046.50, 783.99
    };
    static constexpr double kChaturangaPattern[12] = {
        261.63, 277.18, 329.63, 349.23, 392.00, 415.30,
        392.00, 349.23, 329.63, 277.18, 261.63, 196.00
    };
    static constexpr double kShatranjPattern[8] = {
        293.66, 349.23, 440.00, 523.25, 493.88, 440.00, 392.00, 329.63
    };

    const double* pattern = kShatranjPattern;
    int patternLen = 8;
    double droneFreq = 146.83;
    double noteDurationSec = 0.72;
    bool isJoyful = false;

    if (gameTypeOrdinal == 1) { // LUDO
        pattern = kLudoPattern;
        patternLen = 12;
        droneFreq = 130.81;
        noteDurationSec = 0.34;
        isJoyful = true;
    } else if (gameTypeOrdinal == 2) { // CHATURANGA
        pattern = kChaturangaPattern;
        patternLen = 12;
        droneFreq = 130.81;
        noteDurationSec = 0.68;
        isJoyful = false;
    }

    int safeIdx = ((noteIndex % patternLen) + patternLen) % patternLen;
    double melodyFreq = pattern[safeIdx];
    double harmonicFreq = isJoyful ? melodyFreq * 2.0 : droneFreq * 1.5;

    int sampleCount = std::max(1, static_cast<int>(sampleRate * noteDurationSec));
    std::vector<int16_t> out(sampleCount);
    synthesizeMusicNote(
        out.data(),
        sampleCount,
        sampleRate,
        melodyFreq,
        harmonicFreq,
        droneFreq,
        isJoyful,
        globalSampleStart
    );
    return out;
}

} // namespace cosmium::audio
