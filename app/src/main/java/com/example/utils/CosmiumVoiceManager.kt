package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class CosmiumVoiceManager(context: Context) {
    private companion object {
        const val TAG = "CosmiumVoiceManager"
    }
    private var tts: TextToSpeech? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    private var initCompleted = false

    @Volatile
    private var pendingLocale: Locale = Locale.ENGLISH

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                initCompleted = true
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    tts?.setSpeechRate(0.98f)
                    tts?.setPitch(0.90f)
                    applyLocaleSafely(pendingLocale)
                } else {
                    Log.w(TAG, "TTS Initialization failed with status=$status")
                }
            }.apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        _currentRms.value = 0.85f
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentRms.value = 0.0f
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentRms.value = 0.0f
                    }
                })
            }
        }.onFailure { err ->
            initCompleted = true
            Log.w(TAG, "TTS unavailable in current environment: ${err.message}")
        }
    }

    private fun resolveLocale(langCode: String): Locale {
        return when (langCode.lowercase()) {
            "hi", "hindi" -> Locale("hi", "IN")
            "bn", "bengali" -> Locale("bn", "IN")
            "gu", "gujarati" -> Locale("gu", "IN")
            "ta", "tamil" -> Locale("ta", "IN")
            "te", "telugu" -> Locale("te", "IN")
            "mr", "marathi" -> Locale("mr", "IN")
            "kn", "kannada" -> Locale("kn", "IN")
            "ml", "malayalam" -> Locale("ml", "IN")
            "pa", "punjabi" -> Locale("pa", "IN")
            "ur", "urdu" -> Locale("ur", "IN")
            "sa", "sanskrit" -> Locale("sa", "IN")
            "or", "odia" -> Locale("or", "IN")
            "as", "assamese" -> Locale("as", "IN")
            "ne", "nepali" -> Locale("ne", "IN")
            "kok", "konkani" -> Locale("kok", "IN")
            "sd", "sindhi" -> Locale("sd", "IN")
            "ks", "kashmiri" -> Locale("ks", "IN")
            "doi", "dogri" -> Locale("doi", "IN")
            "mai", "maithili" -> Locale("mai", "IN")
            "sat", "santali" -> Locale("sat", "IN")
            "mni", "manipuri" -> Locale("mni", "IN")
            "brx", "bodo" -> Locale("brx", "IN")
            else -> Locale.ENGLISH
        }
    }

    private fun applyLocaleSafely(locale: Locale): Boolean {
        val engine = tts ?: return false
        return runCatching {
            val res = engine.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Locale ${locale.toLanguageTag()} unavailable on device TTS, using English fallback")
                engine.setLanguage(Locale.ENGLISH)
                false
            } else {
                true
            }
        }.getOrElse {
            runCatching { engine.setLanguage(Locale.ENGLISH) }
            false
        }
    }

    fun setLanguage(langCode: String) {
        val locale = resolveLocale(langCode)
        pendingLocale = locale
        if (isInitialized) {
            applyLocaleSafely(locale)
        }
    }

    suspend fun prepareLanguageForOffline(langCode: String): Boolean {
        val locale = resolveLocale(langCode)
        pendingLocale = locale
        var waited = 0
        while (!initCompleted && waited < 12) {
            kotlinx.coroutines.delay(100L)
            waited++
        }
        return if (isInitialized) {
            applyLocaleSafely(locale)
        } else {
            false
        }
    }

    fun speak(text: String) {
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), "Code snippet provided.")
            .replace(Regex("[*#_`~>]+"), "")
            .trim()

        if (cleanText.isBlank()) return

        if (isInitialized) {
            val params = android.os.Bundle()
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "cosmium_utterance_1")
        } else {
            Log.w(TAG, "TTS not yet initialized")
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentRms.value = 0f
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
