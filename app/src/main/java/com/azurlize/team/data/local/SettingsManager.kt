package com.azurlize.team.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    SYSTEM, LIGHT, DARK, AMOLED
}

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
            AppTheme.valueOf(themeName)
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    val isAppLockEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { it[IS_APP_LOCK_ENABLED] ?: false }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockPin(pin: String) {
        context.dataStore.edit { it[APP_LOCK_PIN] = pin }
    }

    val appLockPinFlow: Flow<String?> = context.dataStore.data
        .map { it[APP_LOCK_PIN] }

    val screenshotProtectionFlow: Flow<Boolean> = context.dataStore.data
        .map { it[SCREENSHOT_PROTECTION] ?: false }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        context.dataStore.edit { it[SCREENSHOT_PROTECTION] = enabled }
    }
}
