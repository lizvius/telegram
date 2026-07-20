package com.azurlize.team.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class EditProfileViewModel : ViewModel() {
    private val repository = TelegramRepository.getInstance()

    val me = repository.me
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _updateResult = MutableStateFlow<Boolean?>(null)
    val updateResult: StateFlow<Boolean?> = _updateResult

    fun updateProfile(firstName: String, lastName: String, bio: String, username: String) {
        _loading.value = true
        _updateResult.value = null

        viewModelScope.launch {
            repository.setName(firstName, lastName) { nameSuccess ->
                repository.setBio(bio) { bioSuccess ->
                    repository.setUsername(username) { usernameSuccess ->
                        _loading.value = false
                        _updateResult.value = nameSuccess && bioSuccess && usernameSuccess
                    }
                }
            }
        }
    }

    fun updateProfilePhoto(path: String) {
        _loading.value = true
        repository.setProfilePhoto(path) { success ->
            _loading.value = false
            _updateResult.value = success
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }
}
