#ifndef COSMIUM_AUDIO_H
#define COSMIUM_AUDIO_H

#include <cstdint>
#include <vector>

namespace cosmium::audio {

std::vector<int16_t> synthesizeWoodyTock(
    int sampleRate,
    double durationSec,
    double baseFreq,
    double sweep,
    double volume
);

std::vector<int16_t> synthesizeTempleChime(
    int sampleRate,
    double durationSec,
    double fundamental
);

std::vector<int16_t> synthesizeVictorySwell(
    int sampleRate,
    double durationSec
);

void synthesizeMusicNote(
    int16_t* outBuffer,
    int sampleCount,
    int sampleRate,
    double melodyFreq,
    double harmonicFreq,
    double droneFreq,
    bool isJoyful,
    int64_t globalSampleStart
);

/**
 * Generates one procedural ambient pattern note for a game mode:
 * gameTypeOrdinal: 0 = SHATRANJ, 1 = LUDO, 2 = CHATURANGA.
 */
std::vector<int16_t> synthesizeGameMusicStep(
    int gameTypeOrdinal,
    int noteIndex,
    int sampleRate,
    int64_t globalSampleStart
);

} // namespace cosmium::audio

#endif // COSMIUM_AUDIO_H
