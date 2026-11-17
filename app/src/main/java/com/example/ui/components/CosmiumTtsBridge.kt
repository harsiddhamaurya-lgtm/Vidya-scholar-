package com.example.ui.components

import android.util.Log

object CosmiumTtsBridge {
    private const val TAG = "CosmiumTtsBridge"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("cosmium_native")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load cosmium_native library", e)
        }
    }

    external fun nativeCreate(): Long
    external fun nativeInit(handle: Long, modelPath: String, voicesPath: String, tokensPath: String): Boolean
    external fun nativeSynthesize(handle: Long, text: String)
    external fun nativeGetRms(handle: Long): Float
    external fun nativeIsSpeaking(handle: Long): Boolean
    external fun nativeDestroy(handle: Long)
}
