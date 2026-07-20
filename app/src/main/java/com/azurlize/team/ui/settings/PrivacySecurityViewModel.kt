package com.azurlize.team.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.data.local.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrivacySecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    val isAppLockEnabled = settingsManager.isAppLockEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val screenshotProtection = settingsManager.screenshotProtectionFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAppLockEnabled(enabled)
        }
    }

    fun setScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setScreenshotProtection(enabled)
        }
    }
}
