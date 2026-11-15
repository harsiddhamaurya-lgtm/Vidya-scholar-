package com.example.utils

object NativeCoreBridge {
    private val loaded: Boolean by lazy {
        runCatching {
            System.loadLibrary("cosmium_native")
            true
        }.getOrDefault(false)
    }

    fun isAvailable(): Boolean = loaded

    external fun nativeComputeChessBotMove(
        boardArray: IntArray,
        turnSide: Int,
        difficulty: Int
    ): Int

    external fun nativeSynthesizeWoodyTock(
        sampleRate: Int,
        durationSec: Double,
        baseFreq: Double,
        sweep: Double,
        volume: Double
    ): ShortArray

    external fun nativeSynthesizeTempleChime(
        sampleRate: Int,
        durationSec: Double,
        fundamental: Double
    ): ShortArray

    external fun nativeSynthesizeVictorySwell(
        sampleRate: Int,
        durationSec: Double
    ): ShortArray

    external fun nativeSynthesizeMusicNote(
        outBuffer: ShortArray,
        sampleRate: Int,
        melodyFreq: Double,
        harmonicFreq: Double,
        droneFreq: Double,
        isJoyful: Boolean,
        globalSampleStart: Long
    )
}
