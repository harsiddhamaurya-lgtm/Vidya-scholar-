package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tactile & Acoustic Feedback grounded in warm, organic Indo-Futurism textures:
 * - Game Arena Sound Hooks (muted automatically when GameMusicPlayer.isMuted is enabled):
 *   - Piece Move: warm dense wooden/stone "thock" (~85ms)
 *   - Dice / Pasa Roll: crisp organic ivory/wood tumbling rattle (~240ms)
 *   - Capture / Safe Zone: short resonant metallic bronze chime (<290ms)
 *   - Victory State: brief celebratory sitar/tanpura harmonic swell (~640ms)
 * - App-wide quiet UX feedback (<300ms) while keeping routine tab navigation silent.
 */
object SoundHelper {
    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Deep, low-register woody "tock" for intentional UI selections & board piece moves (~85ms). */
    fun playWoodyTock(context: Context) {
        vibrate(context, 12)
        playSynthesizedWoodyTock()
    }

    /** Soft resonant temple chime (<280ms decay) for app completions & quiz mastery. */
    fun playTempleChime(context: Context) {
        vibrate(context, 28)
        playSynthesizedTempleChime()
    }

    /** Quiet light haptic confirmation for routine toggles / chip taps without audio clutter. */
    fun playClick(context: Context) {
        vibrate(context, 10)
    }

    /** Woody tock paired with tactile feedback when entering a module or game hub. */
    fun playFolderOpen(context: Context) {
        playWoodyTock(context)
    }

    /** Routine navigation swipe stays acoustically silent to respect user calm. */
    fun playSwipe(context: Context) {
        vibrate(context, 6)
    }

    // --- GAME ZONE LOW-LATENCY SOUND HOOKS (Sync with Arena Mute Toggle) ---

    /** Warm, dense wooden/stone "thock" when a piece lands on a square. */
    fun playArenaMove(context: Context) {
        vibrate(context, 14)
        if (GameMusicPlayer.isMuted) return
        playSynthesizedWoodyTock(baseFreq = 115.0, sweep = 95.0, volume = 0.36)
    }

    /** Subtle light wood tap for individual Ludo hopping steps. */
    fun playArenaHopTick(context: Context) {
        vibrate(context, 6)
        if (GameMusicPlayer.isMuted) return
        playSynthesizedWoodyTock(baseFreq = 165.0, sweep = 70.0, durationSec = 0.045, volume = 0.20)
    }

    /** Crisp organic rattle mimicking carved ivory or wooden Pasa tumbling on a board. */
    fun playArenaDiceRattle(context: Context) {
        vibrate(context, 18)
        if (GameMusicPlayer.isMuted) return
        audioScope.launch {
            val offsets = listOf(0L, 48L, 92L, 148L)
            val freqs = listOf(290.0, 340.0, 260.0, 315.0)
            for (idx in offsets.indices) {
                if (idx > 0) delay(offsets[idx] - offsets[idx - 1])
                if (GameMusicPlayer.isMuted) break
                val buffer = buildWoodyClickPcm(
                    sampleRate = 22050,
                    durationSec = 0.036,
                    baseFreq = freqs[idx] + Random.nextInt(-18, 18),
                    sweep = 140.0,
                    volume = 0.24 - idx * 0.02
                )
                writeOneShotPcm(22050, buffer)
            }
        }
    }

    /** Short, resonant metallic bronze bell chime on capturing a piece or landing on a safe star/lotus tile. */
    fun playArenaCaptureOrSafeChime(context: Context, isCapture: Boolean = true) {
        vibrate(context, if (isCapture) 26 else 18)
        if (GameMusicPlayer.isMuted) return
        playSynthesizedTempleChime(
            fundamental = if (isCapture) 587.33 else 659.25, // Warm D5 / E5 bronze chime
            durationSec = 0.27
        )
    }

    /** Celebratory, brief sitar/tanpura harmonic swell on victory. */
    fun playArenaVictorySwell(context: Context) {
        vibrate(context, 42)
        if (GameMusicPlayer.isMuted) return
        audioScope.launch {
            val sampleRate = 22050
            val durationSec = 0.65
            val buffer = if (NativeCoreBridge.isAvailable()) {
                NativeCoreBridge.nativeSynthesizeVictorySwell(sampleRate, durationSec)
            } else {
                val numSamples = (sampleRate * durationSec).toInt()
                val out = ShortArray(numSamples)
                val freqs = doubleArrayOf(261.63, 392.00, 523.25, 659.25)
                val entryTimes = doubleArrayOf(0.0, 0.06, 0.13, 0.21)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var mix = 0.0
                    for (k in freqs.indices) {
                        val dt = t - entryTimes[k]
                        if (dt > 0.0) {
                            val env = (dt / 0.028).coerceIn(0.0, 1.0) * exp(-3.8 * dt)
                            val f = freqs[k]
                            val voice = sin(2.0 * PI * f * dt) * 0.58 +
                                sin(2.0 * PI * (f * 2.0) * dt) * 0.26 +
                                sin(2.0 * PI * (f * 3.0) * dt) * 0.16
                            mix += voice * env
                        }
                    }
                    val sample = (mix * 0.22).coerceIn(-0.92, 0.92)
                    out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                out
            }
            writeOneShotPcm(sampleRate, buffer)
        }
    }

    private fun playSynthesizedWoodyTock(
        baseFreq: Double = 125.0,
        sweep: Double = 85.0,
        durationSec: Double = 0.085,
        volume: Double = 0.32
    ) {
        audioScope.launch {
            val sampleRate = 22050
            val buffer = buildWoodyClickPcm(sampleRate, durationSec, baseFreq, sweep, volume)
            writeOneShotPcm(sampleRate, buffer)
        }
    }

    private fun buildWoodyClickPcm(
        sampleRate: Int,
        durationSec: Double,
        baseFreq: Double,
        sweep: Double,
        volume: Double
    ): ShortArray {
        if (NativeCoreBridge.isAvailable()) {
            return NativeCoreBridge.nativeSynthesizeWoodyTock(sampleRate, durationSec, baseFreq, sweep, volume)
        }
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = baseFreq + sweep * exp(-55.0 * t)
            val attack = (t / 0.004).coerceIn(0.0, 1.0)
            val decay = exp(-42.0 * t)
            val body = sin(2.0 * PI * freq * t) * 0.8 +
                sin(2.0 * PI * (freq * 1.48) * t) * 0.2
            val sample = (body * attack * decay * volume).coerceIn(-0.9, 0.9)
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun playSynthesizedTempleChime(
        fundamental: Double = 528.0,
        durationSec: Double = 0.28
    ) {
        audioScope.launch {
            val sampleRate = 22050
            val buffer = if (NativeCoreBridge.isAvailable()) {
                NativeCoreBridge.nativeSynthesizeTempleChime(sampleRate, durationSec, fundamental)
            } else {
                val numSamples = (sampleRate * durationSec).toInt()
                val out = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val attack = (t / 0.008).coerceIn(0.0, 1.0)
                    val env = attack * exp(-14.5 * t)
                    val overtoneEnv = attack * exp(-22.0 * t)
                    val wave = sin(2.0 * PI * fundamental * t) * 0.65 * env +
                        sin(2.0 * PI * (fundamental * 2.0) * t) * 0.22 * overtoneEnv +
                        sin(2.0 * PI * (fundamental * 2.76) * t) * 0.13 * overtoneEnv
                    val sample = (wave * 0.26).coerceIn(-0.9, 0.9)
                    out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                out
            }
            writeOneShotPcm(sampleRate, buffer)
        }
    }

    private fun writeOneShotPcm(sampleRate: Int, pcm: ShortArray) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((pcm.size * 1000L) / sampleRate + 25L)
        } catch (_: Exception) {
            // Gracefully ignore when audio device unavailable
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun vibrate(context: Context, duration: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (_: Exception) {
            // Ignore if vibration unavailable
        }
    }
}
