package com.example.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckHelper {
    private const val TAG = "AppCheckHelper"

    @Volatile
    private var configured = false

    fun configureAppCheck(context: Context, intent: Intent?) {
        if (configured) return
        try {
            val firebaseAppCheck = Firebase.appCheck
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            configured = true
            Log.d(TAG, "Initialized Debug App Check Provider Factory")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize App Check", t)
        }
    }
}
