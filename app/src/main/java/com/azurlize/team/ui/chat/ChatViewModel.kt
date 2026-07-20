package com.azurlize.team.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class ChatViewModel : ViewModel() {
    private val repository = TelegramRepository.getInstance()

    private val _chat = MutableStateFlow<TdApi.Chat?>(null)
    val chat: StateFlow<TdApi.Chat?> = _chat.asStateFlow()

    private var currentChatId: Long = 0
    private var currentTopicId: Int = 0
    
    private val _searchResults = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val searchResults: StateFlow<List<TdApi.Message>> = _searchResults.asStateFlow()

    private val _messagesList = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val messages: StateFlow<List<TdApi.Message>> = _messagesList.asStateFlow()

    val typingActions: StateFlow<List<TdApi.ChatAction>> = repository.chatTyping.map {
        it[currentChatId] ?: emptyList()
    }.let { flow ->
        val state = MutableStateFlow<List<TdApi.ChatAction>>(emptyList())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state.asStateFlow()
    }

    val fileUpdates = repository.fileUpdates
    val downloadProgress = repository.downloadProgress
    val me = repository.me
    val connectionState = repository.connectionState

    private var messageCollectionJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                if (state is TdApi.ConnectionStateReady) {
                    loadMessages()
                }
            }
        }
    }

    fun setChatId(chatId: Long, topicId: Int = 0) {
        currentChatId = chatId
        currentTopicId = topicId
        repository.getChat(chatId) {
            _chat.value = it
        }
        repository.openChat(chatId)
        
        messageCollectionJob?.cancel()
        
        loadMessages()
        if (topicId == 0) {
            messageCollectionJob = viewModelScope.launch {
                repository.messages.collect { map ->
                    _messagesList.value = map[chatId] ?: emptyList()
                }
            }
        } else {
            messageCollectionJob = viewModelScope.launch {
                repository.messages.collect { map ->
                    val chatMsgs = map[chatId] ?: emptyList()
                    _messagesList.value = chatMsgs.filter { msg ->
                        (msg.topicId as? TdApi.MessageTopicForum)?.forumTopicId == topicId
                    }
                }
            }
        }
    }

    fun searchMessages(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        repository.searchMessages(currentChatId, query) { results ->
            _searchResults.value = results
        }
    }

    fun loadMessages() {
        if (currentChatId == 0L) return
        val lastMessageId = _messagesList.value.lastOrNull()?.id ?: 0L
        if (currentTopicId != 0) {
            repository.getForumTopicHistory(currentChatId, currentTopicId, lastMessageId)
        } else {
            repository.loadChatHistory(currentChatId, lastMessageId)
        }
    }

    fun sendMessage(text: String) {
        if (currentChatId == 0L || text.isBlank()) return
        if (currentTopicId != 0) {
            repository.sendTopicMessage(currentChatId, currentTopicId, text)
        } else {
            repository.sendMessage(currentChatId, text)
        }
    }

    fun sendPhoto(path: String) {
        if (currentChatId == 0L) return
        repository.sendPhoto(currentChatId, path)
    }

    fun sendVideo(path: String) {
        if (currentChatId == 0L) return
        repository.sendVideo(currentChatId, path)
    }

    fun sendDocument(path: String) {
        if (currentChatId == 0L) return
        repository.sendDocument(currentChatId, path)
    }

    fun sendAudio(path: String) {
        if (currentChatId == 0L) return
        repository.sendAudio(currentChatId, path)
    }

    fun sendVoiceNote(path: String) {
        if (currentChatId == 0L) return
        repository.sendVoiceNote(currentChatId, path)
    }

    fun getUser(userId: Long) = repository.getUser(userId)
    fun getSupergroup(supergroupId: Long) = repository.getSupergroup(supergroupId)
    fun getBasicGroup(basicGroupId: Long) = repository.getBasicGroup(basicGroupId)
    fun getFile(fileId: Int) = repository.getFile(fileId)
    fun downloadFile(fileId: Int) = repository.downloadFile(fileId)

    override fun onCleared() {
        super.onCleared()
        repository.onDestroy()
    }
}
