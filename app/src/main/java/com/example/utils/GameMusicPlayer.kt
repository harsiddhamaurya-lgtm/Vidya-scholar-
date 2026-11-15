package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.viewmodel.GameType
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 100% Offline Procedural Ambient Music Synthesizer.
 * Generates distinct atmospheric soundscapes for each game:
 * - SHATRANJ (Chess): Serious, contemplative minor-key cello/piano meditation.
 * - CHATURANGA: Ancient, serious, regal tanpura-like drone with dark raga phrasing.
 * - LUDO (Pachisi): Joyful, bouncy, bright major pentatonic marimba/kalimba melody.
 */
object GameMusicPlayer {
    private var musicJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    var isMuted: Boolean = false
        private set

    @Volatile
    private var currentGameType: GameType? = null

    fun startMusic(gameType: GameType) {
        if (currentGameType == gameType && musicJob?.isActive == true) return
        stopMusic()
        currentGameType = gameType
        if (isMuted) return

        musicJob = scope.launch {
            runSynthLoop(gameType)
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun toggleMute(gameType: GameType): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            stopMusic()
        } else {
            currentGameType = null
            startMusic(gameType)
        }
        return isMuted
    }

    private suspend fun runSynthLoop(gameType: GameType) {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }

        try {
            audioTrack.play()

            // Select musical scale, tempo, and drone frequencies per game
            val pattern: List<Double>
            val droneFreq: Double
            val noteDurationSec: Double
            val isJoyful: Boolean

            when (gameType) {
                GameType.LUDO -> {
                    // Joyful C Major Pentatonic bouncy melody (C5, E5, G5, A5, G5, E5, D5, C5, F5, A5, C6, G5)
                    pattern = listOf(523.25, 659.25, 783.99, 880.00, 783.99, 659.25, 587.33, 523.25, 698.46, 880.00, 1046.50, 783.99)
                    droneFreq = 130.81 // Warm C3 bass pulse
                    noteDurationSec = 0.34
                    isJoyful = true
                }
                GameType.CHATURANGA -> {
                    // Serious & Ancient Raga Bhairav / Regal Drone (Sa, komal Re, Ga, Ma, Pa, komal Dha, Sa)
                    pattern = listOf(261.63, 277.18, 329.63, 349.23, 392.00, 415.30, 392.00, 349.23, 329.63, 277.18, 261.63, 196.00)
                    droneFreq = 130.81 // Deep Sa drone (C3 + G3 fifth harmonic)
                    noteDurationSec = 0.68
                    isJoyful = false
                }
                GameType.SHATRANJ -> {
                    // Serious Strategic D Minor / Dorian Contemplative Chords (D4, F4, A4, C5, B4, A4, G4, E4)
                    pattern = listOf(293.66, 349.23, 440.00, 523.25, 493.88, 440.00, 392.00, 329.63)
                    droneFreq = 146.83 // Deep D3 strategic drone
                    noteDurationSec = 0.72
                    isJoyful = false
                }
            }

            val samplesPerNote = (sampleRate * noteDurationSec).toInt()
            val pcmBuffer = ShortArray(samplesPerNote)
            var noteIndex = 0
            var globalSample = 0L

            while (currentCoroutineContext().isActive && !isMuted) {
                val melodyFreq = pattern[noteIndex % pattern.size]
                val harmonicFreq = if (isJoyful) melodyFreq * 2.0 else droneFreq * 1.5

                if (NativeCoreBridge.isAvailable()) {
                    NativeCoreBridge.nativeSynthesizeMusicNote(
                        pcmBuffer,
                        sampleRate,
                        melodyFreq,
                        harmonicFreq,
                        droneFreq,
                        isJoyful,
                        globalSample
                    )
                } else {
                    for (i in 0 until samplesPerNote) {
                        val tLocal = i.toDouble() / sampleRate
                        val tGlobal = (globalSample + i).toDouble() / sampleRate

                        val attack = (tLocal / 0.025).coerceIn(0.0, 1.0)
                        val decayRate = if (isJoyful) 5.5 else 2.2
                        val env = attack * exp(-decayRate * tLocal)

                        val melodyWave = sin(2.0 * PI * melodyFreq * tLocal) * 0.65 +
                            sin(2.0 * PI * harmonicFreq * tLocal) * (if (isJoyful) 0.18 else 0.12)

                        val droneEnv = if (isJoyful) 0.25 else 0.42
                        val droneWave = sin(2.0 * PI * droneFreq * tGlobal) * droneEnv +
                            sin(2.0 * PI * (droneFreq * 1.5) * tGlobal) * (droneEnv * 0.45)

                        val mixed = (melodyWave * env + droneWave) * 0.14
                        pcmBuffer[i] = (mixed.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
                    }
                }

                audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                globalSample += samplesPerNote
                noteIndex++
            }
        } catch (_: CancellationException) {
            // Normal coroutine cancellation
        } catch (_: Exception) {
            // Audio device fallback
        } finally {
            try {
                audioTrack.pause()
                audioTrack.flush()
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }
}
