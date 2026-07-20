package com.azurlize.team.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class HomeViewModel : ViewModel() {
    private val repository = TelegramRepository.getInstance()

    enum class ChatCategory { ALL, PERSONAL, GROUPS, CHANNELS }

    private val _currentCategory = MutableStateFlow(ChatCategory.ALL)
    val currentCategory: StateFlow<ChatCategory> = _currentCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val chats: StateFlow<List<TdApi.Chat>> = repository.chats
    val connectionState: StateFlow<TdApi.ConnectionState> = repository.connectionState
    val me: StateFlow<TdApi.User?> = repository.me
    val fileUpdates: StateFlow<TdApi.File?> = repository.fileUpdates

    private val _filteredChats = MutableStateFlow<List<TdApi.Chat>>(emptyList())
    val filteredChats: StateFlow<List<TdApi.Chat>> = _filteredChats.asStateFlow()

    init {
        viewModelScope.launch {
            combine(chats, searchQuery, _currentCategory) { chatList, query, category ->
                val filteredByCategory = when (category) {
                    ChatCategory.ALL -> chatList
                    ChatCategory.PERSONAL -> chatList.filter { it.type is TdApi.ChatTypePrivate }
                    ChatCategory.GROUPS -> chatList.filter { chat ->
                        when (val type = chat.type) {
                            is TdApi.ChatTypeBasicGroup -> true
                            is TdApi.ChatTypeSupergroup -> !repository.getSupergroup(type.supergroupId).let { it?.isChannel ?: false }
                            else -> false
                        }
                    }
                    ChatCategory.CHANNELS -> chatList.filter { chat ->
                        when (val type = chat.type) {
                            is TdApi.ChatTypeSupergroup -> repository.getSupergroup(type.supergroupId).let { it?.isChannel ?: false }
                            else -> false
                        }
                    }
                }

                if (query.isEmpty()) {
                    filteredByCategory
                } else {
                    filteredByCategory.filter { it.title.contains(query, ignoreCase = true) }
                }
            }.collect {
                _filteredChats.value = it
            }
        }
    }

    fun setCategory(category: ChatCategory) {
        _currentCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getUser(userId: Long) = repository.getUser(userId)
    fun getSupergroup(supergroupId: Long) = repository.getSupergroup(supergroupId)
    fun getBasicGroup(basicGroupId: Long) = repository.getBasicGroup(basicGroupId)
    fun getFile(fileId: Int) = repository.getFile(fileId)
    fun downloadFile(fileId: Int, priority: Int = 1) = repository.downloadFile(fileId, priority)

    override fun onCleared() {
        super.onCleared()
        repository.onDestroy()
    }
}
