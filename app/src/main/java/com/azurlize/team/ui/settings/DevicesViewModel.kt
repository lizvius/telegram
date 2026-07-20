package com.azurlize.team.ui.settings

import androidx.lifecycle.ViewModel
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.TdApi

class DevicesViewModel : ViewModel() {
    private val repository = TelegramRepository.getInstance()

    private val _sessions = MutableStateFlow<List<TdApi.Session>>(emptyList())
    val sessions: StateFlow<List<TdApi.Session>> = _sessions

    init {
        fetchSessions()
    }

    fun fetchSessions() {
        repository.getActiveSessions { result ->
            _sessions.value = result?.sessions?.toList() ?: emptyList()
        }
    }

    fun terminateSession(sessionId: Long) {
        repository.terminateSession(sessionId) { success ->
            if (success) {
                fetchSessions()
            }
        }
    }

    fun terminateAllOtherSessions() {
        repository.terminateAllOtherSessions { success ->
            if (success) {
                fetchSessions()
            }
        }
    }
}
