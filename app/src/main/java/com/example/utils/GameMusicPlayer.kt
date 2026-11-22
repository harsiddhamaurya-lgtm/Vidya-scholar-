package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.viewmodel.GameType
import kotlinx.coroutines.*

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

            var noteIndex = 0
            var globalSample = 0L

            while (currentCoroutineContext().isActive && !isMuted) {
                val pcmBuffer = NativeCoreBridge.synthesizeGameMusicStep(
                    gameTypeOrdinal = gameType.ordinal,
                    noteIndex = noteIndex,
                    sampleRate = sampleRate,
                    globalSampleStart = globalSample
                )
                audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                globalSample += pcmBuffer.size
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
