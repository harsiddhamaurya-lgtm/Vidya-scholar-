package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.security.LocalProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    DARK,
    LIGHT,
    EINK
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val profileStore by lazy { LocalProfileStore(application.applicationContext) }

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun loadSavedThemeMode(): AppThemeMode {
        val savedName = profileStore.getThemeMode(AppThemeMode.LIGHT.name)
        return runCatching { AppThemeMode.valueOf(savedName) }.getOrDefault(AppThemeMode.LIGHT)
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        profileStore.saveThemeMode(mode.name)
    }

    companion object {
        fun detectSmartUiMode(context: Context): AppThemeMode = AppThemeMode.LIGHT
    }
}
