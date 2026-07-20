package com.azurlize.team.ui.settings

import androidx.lifecycle.ViewModel
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.TdApi

class StorageViewModel : ViewModel() {
    private val repository = TelegramRepository.getInstance()

    private val _storageStats = MutableStateFlow<TdApi.StorageStatistics?>(null)
    val storageStats: StateFlow<TdApi.StorageStatistics?> = _storageStats

    init {
        fetchStorageStats()
    }

    fun fetchStorageStats() {
        repository.getStorageStatistics { stats ->
            _storageStats.value = stats
        }
    }

    fun clearCache() {
        repository.optimizeStorage { success ->
            if (success) {
                fetchStorageStats()
            }
        }
    }
}
