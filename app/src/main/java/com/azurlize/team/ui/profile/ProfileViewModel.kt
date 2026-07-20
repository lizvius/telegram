package com.azurlize.team.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class ProfileViewModel : ViewModel() {
    private val repository = TelegramRepository.getInstance()

    val me = repository.me
    
    private val _userFullInfo = MutableStateFlow<TdApi.UserFullInfo?>(null)
    val userFullInfo: StateFlow<TdApi.UserFullInfo?> = _userFullInfo

    fun fetchUserFullInfo(userId: Long) {
        repository.getUserFullInfo(userId) { info ->
            _userFullInfo.value = info
        }
    }

    fun getFile(fileId: Int) {
        repository.downloadFile(fileId)
    }

    fun logout(callback: (Boolean) -> Unit) {
        repository.logout(callback)
    }
}
