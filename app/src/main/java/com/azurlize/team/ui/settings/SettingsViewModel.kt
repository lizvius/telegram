package com.azurlize.team.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.data.local.AppTheme
import com.azurlize.team.data.local.SettingsManager
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelegramRepository.getInstance()
    private val settingsManager = SettingsManager(application)

    val theme = settingsManager.themeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppTheme.SYSTEM
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsManager.setTheme(theme)
        }
    }

    fun logout(callback: (Boolean) -> Unit) {
        repository.logout(callback)
    }
}
