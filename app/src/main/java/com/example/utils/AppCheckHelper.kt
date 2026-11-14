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

    fun configureAppCheck(context: Context, intent: Intent?) {
        try {
            val firebaseAppCheck = Firebase.appCheck
            
            // For development, use DebugAppCheckProviderFactory
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Initialized Debug App Check Provider Factory")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize App Check", e)
        }
    }
}
