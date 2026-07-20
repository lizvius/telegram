package com.azurlize.team.telegram.repository

import android.util.Log
import com.azurlize.team.telegram.client.TelegramClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

private const val TAG = "TelegramRepository"

class TelegramRepository private constructor() {
    private val client = TelegramClientProvider.getClient()

    private val _chats = MutableStateFlow<List<TdApi.Chat>>(emptyList())
    val chats: StateFlow<List<TdApi.Chat>> = _chats.asStateFlow()

    private val _contacts = MutableStateFlow<List<TdApi.User>>(emptyList())
    val contacts: StateFlow<List<TdApi.User>> = _contacts.asStateFlow()

    private val _connectionState = MutableStateFlow<TdApi.ConnectionState>(TdApi.ConnectionStateWaitingForNetwork())
    val connectionState: StateFlow<TdApi.ConnectionState> = _connectionState.asStateFlow()

    private val _me = MutableStateFlow<TdApi.User?>(null)
    val me: StateFlow<TdApi.User?> = _me.asStateFlow()

    private val chatMap = mutableMapOf<Long, TdApi.Chat>()
    private val userMap = mutableMapOf<Long, TdApi.User>()
    private val supergroupMap = mutableMapOf<Long, TdApi.Supergroup>()
    private val basicGroupMap = mutableMapOf<Long, TdApi.BasicGroup>()
    private val fileMap = mutableMapOf<Int, TdApi.File>()

    private val _fileUpdates = MutableStateFlow<TdApi.File?>(null)
    val fileUpdates: StateFlow<TdApi.File?> = _fileUpdates.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Int>> = _downloadProgress.asStateFlow()

    private val _messages = MutableStateFlow<Map<Long, List<TdApi.Message>>>(emptyMap())
    val messages: StateFlow<Map<Long, List<TdApi.Message>>> = _messages.asStateFlow()

    private val _chatTyping = MutableStateFlow<Map<Long, List<TdApi.ChatAction>>>(emptyMap())
    val chatTyping: StateFlow<Map<Long, List<TdApi.ChatAction>>> = _chatTyping.asStateFlow()

    init {
        Log.d(TAG, "Initializing TelegramRepository")
        TelegramClientProvider.addUpdateListener(::handleUpdate)
        fetchMe()
        loadInitialChats()
        fetchContacts()
    }

    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateConnectionState -> {
                Log.i(TAG, "NETWORK: Connection state changed to ${update.state}")
                _connectionState.value = update.state
            }
            is TdApi.UpdateNewChat -> {
                Log.i(TAG, "CHAT: New chat added: ${update.chat.id} (${update.chat.title})")
                chatMap[update.chat.id] = update.chat
                updateChatList()
            }
            is TdApi.UpdateChatLastMessage -> {
                Log.d(TAG, "CHAT: Update last message for chat ${update.chatId}")
                chatMap[update.chatId]?.let { chat ->
                    chat.lastMessage = update.lastMessage
                    chat.positions = update.positions
                    updateChatList()
                }
            }
            is TdApi.UpdateChatPosition -> {
                Log.d(TAG, "CHAT: Update chat position for chat ${update.chatId}")
                chatMap[update.chatId]?.let { chat ->
                    val newPositions = chat.positions.toMutableList()
                    val index = newPositions.indexOfFirst { it.list.javaClass == update.position.list.javaClass }
                    if (index != -1) {
                        newPositions[index] = update.position
                    } else {
                        newPositions.add(update.position)
                    }
                    chat.positions = newPositions.toTypedArray()
                    updateChatList()
                }
            }
            is TdApi.UpdateChatReadInbox -> {
                Log.d(TAG, "CHAT: Update read inbox for chat ${update.chatId}")
                chatMap[update.chatId]?.let { chat ->
                    chat.lastReadInboxMessageId = update.lastReadInboxMessageId
                    chat.unreadCount = update.unreadCount
                    updateChatList()
                }
            }
            is TdApi.UpdateChatReadOutbox -> {
                Log.d(TAG, "CHAT: Update read outbox for chat ${update.chatId}")
                chatMap[update.chatId]?.let { chat ->
                    chat.lastReadOutboxMessageId = update.lastReadOutboxMessageId
                    updateChatList()
                }
            }
            is TdApi.UpdateChatUnreadMentionCount -> {
                chatMap[update.chatId]?.let { chat ->
                    chat.unreadMentionCount = update.unreadMentionCount
                    updateChatList()
                }
            }
            is TdApi.UpdateChatTitle -> {
                Log.i(TAG, "CHAT: Update title for chat ${update.chatId}: ${update.title}")
                chatMap[update.chatId]?.let { chat ->
                    chat.title = update.title
                    updateChatList()
                }
            }
            is TdApi.UpdateChatPhoto -> {
                Log.i(TAG, "CHAT: Update photo for chat ${update.chatId}")
                chatMap[update.chatId]?.let { chat ->
                    chat.photo = update.photo
                    updateChatList()
                }
            }
            is TdApi.UpdateChatDraftMessage -> {
                chatMap[update.chatId]?.let { chat ->
                    chat.draftMessage = update.draftMessage
                    chat.positions = update.positions
                    updateChatList()
                }
            }
            is TdApi.UpdateUser -> {
                Log.d(TAG, "USER: Update user ${update.user.id}")
                userMap[update.user.id] = update.user
                if (_me.value?.id == update.user.id) {
                    _me.value = update.user
                }
                updateChatList()
                updateContactsList()
            }
            is TdApi.UpdateUserStatus -> {
                Log.d(TAG, "USER: Update user status ${update.userId}")
                userMap[update.userId]?.let { user ->
                    user.status = update.status
                    if (_me.value?.id == update.userId) {
                        _me.value = user
                    }
                }
                updateChatList()
                updateContactsList()
            }
            is TdApi.UpdateSupergroup -> {
                supergroupMap[update.supergroup.id] = update.supergroup
                updateChatList()
            }
            is TdApi.UpdateBasicGroup -> {
                basicGroupMap[update.basicGroup.id] = update.basicGroup
                updateChatList()
            }
            is TdApi.UpdateFile -> {
                val file = update.file
                Log.d(TAG, "FILE: Update file ${file.id}, local: ${file.local.isDownloadingCompleted}, progress: ${file.local.downloadedSize}/${file.size}")
                fileMap[file.id] = file
                
                if (file.size > 0) {
                    val progress = (file.local.downloadedSize.toDouble() / file.size * 100).toInt()
                    _downloadProgress.value = _downloadProgress.value + (file.id to progress)
                }
                
                _fileUpdates.value = file
            }
            is TdApi.UpdateNewMessage -> {
                val message = update.message
                val chatId = message.chatId
                Log.i(TAG, "MESSAGE: New message in chat $chatId: ${message.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(message) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            }
            is TdApi.UpdateMessageContent -> {
                val chatId = update.chatId
                val messageId = update.messageId
                Log.i(TAG, "MESSAGE: Update content for message $messageId in chat $chatId")
                val currentMessages = _messages.value[chatId] ?: return
                val newMessages = currentMessages.map { 
                    if (it.id == messageId) {
                        it.content = update.newContent
                        it
                    } else it
                }
                _messages.value = _messages.value + (chatId to newMessages)
            }
            is TdApi.UpdateMessageEdited -> {
                val chatId = update.chatId
                val messageId = update.messageId
                Log.i(TAG, "MESSAGE: Message edited $messageId in chat $chatId")
                val currentMessages = _messages.value[chatId] ?: return
                val newMessages = currentMessages.map { 
                    if (it.id == messageId) {
                        it.editDate = update.editDate
                        it.replyMarkup = update.replyMarkup
                        it
                    } else it
                }
                _messages.value = _messages.value + (chatId to newMessages)
            }
            is TdApi.UpdateDeleteMessages -> {
                val chatId = update.chatId
                Log.i(TAG, "MESSAGE: Messages deleted in chat $chatId: ${update.messageIds.joinToString()}")
                val currentMessages = _messages.value[chatId] ?: return
                val deletedIds = update.messageIds.toSet()
                val newMessages = currentMessages.filter { it.id !in deletedIds }
                _messages.value = _messages.value + (chatId to newMessages)
            }
            is TdApi.UpdateMessageSendSucceeded -> {
                val chatId = update.message.chatId
                val oldId = update.oldMessageId
                Log.i(TAG, "MESSAGE: Message send succeeded in chat $chatId, oldId: $oldId, newId: ${update.message.id}")
                val currentMessages = _messages.value[chatId] ?: return
                val newMessages = currentMessages.map { 
                    if (it.id == oldId) update.message else it
                }.distinctBy { it.id }.sortedByDescending { it.date }
                _messages.value = _messages.value + (chatId to newMessages)
            }
            is TdApi.UpdateMessageSendFailed -> {
                val chatId = update.message.chatId
                val oldId = update.oldMessageId
                Log.e(TAG, "MESSAGE: Message send failed in chat $chatId, oldId: $oldId, error: ${update.error.message}")
                val currentMessages = _messages.value[chatId] ?: return
                val newMessages = currentMessages.map { 
                    if (it.id == oldId) update.message else it
                }
                _messages.value = _messages.value + (chatId to newMessages)
            }
            is TdApi.UpdateChatAction -> {
                Log.d(TAG, "CHAT: Action in chat ${update.chatId} by sender ${update.senderId}: ${update.action}")
                _chatTyping.value = _chatTyping.value + (update.chatId to listOf(update.action))
            }
            is TdApi.UpdateChatNotificationSettings -> {
                Log.d(TAG, "CHAT: Update notification settings for chat ${update.chatId}")
                chatMap[update.chatId]?.let { chat ->
                    chat.notificationSettings = update.notificationSettings
                    updateChatList()
                }
            }
            is TdApi.UpdateUserFullInfo -> {
                Log.d(TAG, "USER: Update full info for user ${update.userId}")
                // We don't store full info in userMap as it's a different class, 
                // but we could have a separate map if needed.
            }
            is TdApi.UpdateOption -> {
                Log.d(TAG, "OPTION: Option updated: ${update.name} = ${update.value}")
            }
            is TdApi.UpdateAuthorizationState -> {
                Log.i(TAG, "AUTH: Authorization state changed to ${update.authorizationState}")
                if (update.authorizationState is TdApi.AuthorizationStateReady) {
                    fetchMe()
                    loadInitialChats()
                }
            }
        }
    }

    fun loadChatHistory(chatId: Long, fromMessageId: Long = 0, limit: Int = 50) {
        Log.d(TAG, "DATABASE: Loading chat history for $chatId from message $fromMessageId")
        client.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) { result ->
            if (result is TdApi.Messages) {
                Log.i(TAG, "DATABASE: Loaded ${result.messages.size} messages for chat $chatId")
                val newMessages = result.messages.toList()
                val currentMessages = _messages.value[chatId] ?: emptyList()
                val combined = (currentMessages + newMessages)
                    .distinctBy { it.id }
                    .sortedByDescending { it.date }
                _messages.value = _messages.value + (chatId to combined)
            } else if (result is TdApi.Error) {
                Log.e(TAG, "DATABASE: Error loading chat history: ${result.message}")
            }
        }
    }

    fun sendMessage(chatId: Long, text: String) {
        Log.i(TAG, "UPLOAD: Sending message to chat $chatId")
        val content = TdApi.InputMessageText(TdApi.FormattedText(text, null), null, true)
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Message sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            } else if (result is TdApi.Error) {
                Log.e(TAG, "UPLOAD: Error sending message: ${result.message}")
            }
        }
    }

    fun sendPhoto(chatId: Long, path: String, caption: String = "") {
        Log.i(TAG, "UPLOAD: Sending photo to chat $chatId: $path")
        val inputPhoto = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessagePhoto(inputPhoto, null, null, null, 0, 0, TdApi.FormattedText(caption, null), false, null, false)
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Photo sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            }
        }
    }

    fun sendVideo(chatId: Long, path: String, caption: String = "") {
        Log.i(TAG, "UPLOAD: Sending video to chat $chatId: $path")
        val inputVideo = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessageVideo(inputVideo, null, null, 0, null, 0, 0, 0, true, TdApi.FormattedText(caption, null), false, null, false)
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Video sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            }
        }
    }

    fun sendDocument(chatId: Long, path: String, caption: String = "") {
        Log.i(TAG, "UPLOAD: Sending document to chat $chatId: $path")
        val inputDoc = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessageDocument(inputDoc, null, false, TdApi.FormattedText(caption, null))
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Document sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            }
        }
    }

    fun sendAudio(chatId: Long, path: String, caption: String = "") {
        Log.i(TAG, "UPLOAD: Sending audio to chat $chatId: $path")
        val inputAudio = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessageAudio(inputAudio, null, 0, "", "", TdApi.FormattedText(caption, null))
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Audio sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            }
        }
    }

    fun sendVoiceNote(chatId: Long, path: String, caption: String = "") {
        Log.i(TAG, "UPLOAD: Sending voice note to chat $chatId: $path")
        val inputVoice = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessageVoiceNote(inputVoice, 0, null, TdApi.FormattedText(caption, null), null)
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Voice note sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            }
        }
    }

    fun getChat(chatId: Long, callback: (TdApi.Chat?) -> Unit) {
        val chat = chatMap[chatId]
        if (chat != null) {
            callback(chat)
        } else {
            Log.d(TAG, "DATABASE: Fetching chat $chatId from TDLib")
            client.send(TdApi.GetChat(chatId)) { result ->
                if (result is TdApi.Chat) {
                    chatMap[chatId] = result
                    callback(result)
                } else {
                    Log.e(TAG, "DATABASE: Error fetching chat $chatId: $result")
                    callback(null)
                }
            }
        }
    }

    fun downloadFile(fileId: Int, priority: Int = 1) {
        val file = fileMap[fileId]
        if (file != null && file.local.isDownloadingActive) return
        if (file != null && file.local.canBeDownloaded && !file.local.isDownloadingCompleted) {
            Log.i(TAG, "DOWNLOAD: Starting download for file $fileId")
            client.send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) { /* Result handled by UpdateFile */ }
        } else if (file == null) {
            Log.i(TAG, "DOWNLOAD: Requesting file $fileId metadata and download")
            client.send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) { /* Result handled by UpdateFile */ }
        }
    }

    private fun updateChatList() {
        val sortedChats = chatMap.values
            .filter { chat -> 
                chat.positions.any { it.list is TdApi.ChatListMain }
            }
            .sortedWith { a, b ->
                val posA = a.positions.find { it.list is TdApi.ChatListMain }?.order ?: 0
                val posB = b.positions.find { it.list is TdApi.ChatListMain }?.order ?: 0
                posB.compareTo(posA)
            }
        _chats.value = sortedChats.toList()
    }

    private fun fetchMe() {
        Log.d(TAG, "USER: Fetching current user info")
        client.send(TdApi.GetMe()) { result ->
            if (result is TdApi.User) {
                Log.i(TAG, "USER: Logged in as ${result.firstName} ${result.lastName}")
                _me.value = result
                userMap[result.id] = result
            }
        }
    }

    private fun loadInitialChats() {
        Log.i(TAG, "CHAT: Loading initial chats")
        client.send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "CHAT: Initial chats loading triggered")
            } else if (result is TdApi.Error) {
                Log.e(TAG, "CHAT: Error loading chats: ${result.message}")
            }
        }
    }

    fun fetchContacts() {
        Log.i(TAG, "CONTACT: Fetching contacts")
        client.send(TdApi.GetContacts()) { result ->
            if (result is TdApi.Users) {
                Log.d(TAG, "CONTACT: Found ${result.userIds.size} contacts")
                result.userIds.forEach { userId ->
                    client.send(TdApi.GetUser(userId)) { userResult ->
                        if (userResult is TdApi.User) {
                            userMap[userResult.id] = userResult
                            updateContactsList()
                        }
                    }
                }
            }
        }
    }

    private fun updateContactsList() {
        _contacts.value = userMap.values
            .filter { it.type is TdApi.UserTypeRegular }
            .sortedBy { it.firstName }
    }

    fun searchChats(query: String, callback: (List<TdApi.Chat>) -> Unit) {
        Log.d(TAG, "SEARCH: Searching chats with query: $query")
        client.send(TdApi.SearchChats(query, 20)) { result ->
            if (result is TdApi.Chats) {
                val chats = result.chatIds.toList().mapNotNull { chatMap[it] }
                callback(chats)
            } else {
                callback(emptyList())
            }
        }
    }

    fun searchContacts(query: String, callback: (List<TdApi.User>) -> Unit) {
        Log.d(TAG, "SEARCH: Searching contacts with query: $query")
        client.send(TdApi.SearchContacts(query, 20)) { result ->
            if (result is TdApi.Users) {
                val users = result.userIds.toList().mapNotNull { userMap[it] }
                callback(users)
            } else {
                callback(emptyList())
            }
        }
    }

    fun searchMessages(chatId: Long, query: String, callback: (List<TdApi.Message>) -> Unit) {
        Log.d(TAG, "SEARCH: Searching messages in chat $chatId with query: $query")
        // SearchChatMessages(long chatId, MessageTopic topicId, String query, MessageSender senderId, long fromMessageId, int offset, int limit, SearchMessagesFilter filter)
        client.send(TdApi.SearchChatMessages(chatId, null, query, null, 0, 0, 50, null)) { result ->
            if (result is TdApi.FoundChatMessages) {
                callback(result.messages.toList())
            } else {
                callback(emptyList())
            }
        }
    }

    fun createPrivateChat(userId: Long, callback: (TdApi.Chat?) -> Unit) {
        Log.d(TAG, "NEW_CHAT: Creating private chat with user $userId")
        client.send(TdApi.CreatePrivateChat(userId, false)) { result ->
            if (result is TdApi.Chat) {
                chatMap[result.id] = result
                callback(result)
            } else {
                Log.e(TAG, "NEW_CHAT: Error creating private chat: $result")
                callback(null)
            }
        }
    }

    fun getUserFullInfo(userId: Long, callback: (TdApi.UserFullInfo?) -> Unit) {
        Log.d(TAG, "USER: Getting full info for user $userId")
        client.send(TdApi.GetUserFullInfo(userId)) { result ->
            if (result is TdApi.UserFullInfo) {
                callback(result)
            } else {
                callback(null)
            }
        }
    }

    fun getUser(userId: Long): TdApi.User? = userMap[userId]
    fun getSupergroup(supergroupId: Long): TdApi.Supergroup? = supergroupMap[supergroupId]
    fun getBasicGroup(basicGroupId: Long): TdApi.BasicGroup? = basicGroupMap[basicGroupId]
    fun getFile(fileId: Int): TdApi.File? = fileMap[fileId]

    fun openChat(chatId: Long) {
        Log.d(TAG, "CHAT: Opening chat $chatId")
        client.send(TdApi.OpenChat(chatId)) { result ->
            if (result !is TdApi.Ok) {
                Log.e(TAG, "CHAT: Error opening chat $chatId: $result")
            }
        }
    }

    fun setName(firstName: String, lastName: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "USER: Setting name to $firstName $lastName")
        client.send(TdApi.SetName(firstName, lastName)) { result ->
            callback(result is TdApi.Ok)
        }
    }
    fun setBio(bio: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "USER: Setting bio to $bio")
        client.send(TdApi.SetBio(bio)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun setUsername(username: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "USER: Setting username to $username")
        client.send(TdApi.SetUsername(username)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun setProfilePhoto(path: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "USER: Setting profile photo to $path")
        val inputPhoto = TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(path))
        client.send(TdApi.SetProfilePhoto(inputPhoto, true)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun getActiveSessions(callback: (TdApi.Sessions?) -> Unit) {
        Log.d(TAG, "SESSION: Getting active sessions")
        client.send(TdApi.GetActiveSessions()) { result ->
            if (result is TdApi.Sessions) {
                callback(result)
            } else {
                callback(null)
            }
        }
    }

    fun terminateSession(sessionId: Long, callback: (Boolean) -> Unit) {
        Log.d(TAG, "SESSION: Terminating session $sessionId")
        client.send(TdApi.TerminateSession(sessionId)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun terminateAllOtherSessions(callback: (Boolean) -> Unit) {
        Log.d(TAG, "SESSION: Terminating all other sessions")
        client.send(TdApi.TerminateAllOtherSessions()) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun getStorageStatistics(callback: (TdApi.StorageStatistics?) -> Unit) {
        Log.d(TAG, "STORAGE: Getting storage statistics")
        client.send(TdApi.GetStorageStatistics(0)) { result ->
            if (result is TdApi.StorageStatistics) {
                callback(result)
            } else {
                callback(null)
            }
        }
    }

    fun optimizeStorage(callback: (Boolean) -> Unit) {
        Log.d(TAG, "STORAGE: Optimizing storage")
        // optimizeStorage(size, ttl, count, immunityDelay, fileTypes, chatIds, excludeChatIds, returnStatistics, chatLimit)
        client.send(TdApi.OptimizeStorage(0, 0, 0, 0, null, null, null, false, 0)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun getDatabaseStatistics(callback: (String?) -> Unit) {
        Log.d(TAG, "STORAGE: Getting database statistics")
        client.send(TdApi.GetDatabaseStatistics()) { result ->
            if (result is TdApi.HttpUrl) {
                callback(result.url)
            } else {
                callback(null)
            }
        }
    }

    fun getForumTopics(chatId: Long, query: String = "", callback: (List<TdApi.ForumTopic>) -> Unit) {
        Log.d(TAG, "FORUM: Getting forum topics for chat $chatId")
        client.send(TdApi.GetForumTopics(chatId, query, 0, 0, 0, 100)) { result ->
            if (result is TdApi.ForumTopics) {
                callback(result.topics.toList())
            } else {
                Log.e(TAG, "FORUM: Error getting forum topics: $result")
                callback(emptyList())
            }
        }
    }

    fun createForumTopic(chatId: Long, name: String, callback: (TdApi.ForumTopicInfo?) -> Unit) {
        Log.d(TAG, "FORUM: Creating forum topic '$name' in chat $chatId")
        client.send(TdApi.CreateForumTopic(chatId, name, false, null)) { result ->
            if (result is TdApi.ForumTopicInfo) {
                callback(result)
            } else {
                Log.e(TAG, "FORUM: Error creating forum topic: $result")
                callback(null)
            }
        }
    }

    fun editForumTopic(chatId: Long, topicId: Int, name: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "FORUM: Editing forum topic $topicId in chat $chatId")
        client.send(TdApi.EditForumTopic(chatId, topicId, name, false, 0)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun deleteForumTopic(chatId: Long, topicId: Int, callback: (Boolean) -> Unit) {
        Log.d(TAG, "FORUM: Deleting forum topic $topicId in chat $chatId")
        client.send(TdApi.DeleteForumTopic(chatId, topicId)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun toggleForumTopicIsClosed(chatId: Long, topicId: Int, isClosed: Boolean, callback: (Boolean) -> Unit) {
        Log.d(TAG, "FORUM: Toggling forum topic $topicId isClosed to $isClosed")
        client.send(TdApi.ToggleForumTopicIsClosed(chatId, topicId, isClosed)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun toggleForumTopicIsPinned(chatId: Long, topicId: Int, isPinned: Boolean, callback: (Boolean) -> Unit) {
        Log.d(TAG, "FORUM: Toggling forum topic $topicId isPinned to $isPinned")
        client.send(TdApi.ToggleForumTopicIsPinned(chatId, topicId, isPinned)) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun getForumTopicHistory(chatId: Long, topicId: Int, fromMessageId: Long = 0, limit: Int = 50, callback: (List<TdApi.Message>) -> Unit = {}) {
        Log.d(TAG, "FORUM: Getting topic history for chat $chatId, topic $topicId from message $fromMessageId")
        client.send(TdApi.GetForumTopicHistory(chatId, topicId, fromMessageId, 0, limit)) { result ->
            if (result is TdApi.Messages) {
                val newMessages = result.messages.toList()
                val currentMessages = _messages.value[chatId] ?: emptyList()
                val combined = (currentMessages + newMessages)
                    .distinctBy { it.id }
                    .sortedByDescending { it.date }
                _messages.value = _messages.value + (chatId to combined)
                
                callback(newMessages)
            } else {
                Log.e(TAG, "FORUM: Error getting topic history: $result")
                callback(emptyList())
            }
        }
    }

    fun getChatHistory(chatId: Long, fromMessageId: Long = 0, limit: Int = 50, callback: (List<TdApi.Message>) -> Unit = {}) {
        Log.d(TAG, "CHAT: Getting history for chat $chatId from message $fromMessageId")
        client.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) { result ->
            if (result is TdApi.Messages) {
                val newMessages = result.messages.toList()
                val currentMessages = _messages.value[chatId] ?: emptyList()
                val combined = (currentMessages + newMessages)
                    .distinctBy { it.id }
                    .sortedByDescending { it.date }
                _messages.value = _messages.value + (chatId to combined)
                
                callback(newMessages)
            } else {
                Log.e(TAG, "CHAT: Error getting chat history: $result")
                callback(emptyList())
            }
        }
    }

    fun sendTopicMessage(chatId: Long, topicId: Int, text: String) {
        Log.i(TAG, "UPLOAD: Sending topic message to chat $chatId, topic $topicId")
        val content = TdApi.InputMessageText(TdApi.FormattedText(text, null), null, true)
        val messageTopic = TdApi.MessageTopicForum(topicId)
        client.send(TdApi.SendMessage(chatId, messageTopic, null, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Topic message sent successfully, id: ${result.id}")
                val currentMessages = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to (listOf(result) + currentMessages).distinctBy { it.id }.sortedByDescending { it.date })
            } else if (result is TdApi.Error) {
                Log.e(TAG, "UPLOAD: Error sending topic message: ${result.message}")
            }
        }
    }

    fun replyToMessage(chatId: Long, messageId: Long, text: String) {
        Log.i(TAG, "UPLOAD: Replying to message $messageId in chat $chatId")
        val content = TdApi.InputMessageText(TdApi.FormattedText(text, null), null, true)
        val replyTo = TdApi.InputMessageReplyToMessage(messageId, null, 0, null)
        client.send(TdApi.SendMessage(chatId, null, replyTo, null, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Reply sent successfully, id: ${result.id}")
            } else if (result is TdApi.Error) {
                Log.e(TAG, "UPLOAD: Error replying: ${result.message}")
            }
        }
    }

    fun editMessageText(chatId: Long, messageId: Long, newText: String) {
        Log.i(TAG, "UPLOAD: Editing message $messageId in chat $chatId")
        val content = TdApi.InputMessageText(TdApi.FormattedText(newText, null), null, true)
        client.send(TdApi.EditMessageText(chatId, messageId, null, content)) { result ->
            if (result is TdApi.Message) {
                Log.d(TAG, "UPLOAD: Message edited successfully")
            } else if (result is TdApi.Error) {
                Log.e(TAG, "UPLOAD: Error editing: ${result.message}")
            }
        }
    }

    fun deleteMessages(chatId: Long, messageIds: LongArray, revoke: Boolean) {
        Log.i(TAG, "MESSAGE: Deleting messages ${messageIds.joinToString()} in chat $chatId, revoke: $revoke")
        client.send(TdApi.DeleteMessages(chatId, messageIds, revoke)) { result ->
            if (result !is TdApi.Ok) {
                Log.e(TAG, "MESSAGE: Error deleting messages: $result")
            }
        }
    }

    fun pinChatMessage(chatId: Long, messageId: Long, shouldPin: Boolean) {
        Log.i(TAG, "MESSAGE: Pinning/unpinning message $messageId in chat $chatId: $shouldPin")
        if (shouldPin) {
            client.send(TdApi.PinChatMessage(chatId, messageId, false, false)) { result ->
                if (result !is TdApi.Ok) {
                    Log.e(TAG, "MESSAGE: Error pinning message: $result")
                }
            }
        } else {
            client.send(TdApi.UnpinChatMessage(chatId, messageId)) { result ->
                if (result !is TdApi.Ok) {
                    Log.e(TAG, "MESSAGE: Error unpinning message: $result")
                }
            }
        }
    }

    fun forwardMessages(chatId: Long, fromChatId: Long, messageIds: LongArray) {
        Log.i(TAG, "MESSAGE: Forwarding ${messageIds.size} messages from $fromChatId to $chatId")
        val options = TdApi.MessageSendOptions()
        client.send(TdApi.ForwardMessages(chatId, null, fromChatId, messageIds, options, false, false)) { result ->
            if (result !is TdApi.Messages) {
                Log.e(TAG, "MESSAGE: Error forwarding messages: $result")
            }
        }
    }

    fun logout(callback: (Boolean) -> Unit) {
        Log.i(TAG, "AUTH: Logging out")
        client.send(TdApi.LogOut()) { result ->
            callback(result is TdApi.Ok)
        }
    }

    fun onDestroy() {
        Log.d(TAG, "Destroying TelegramRepository listener")
        TelegramClientProvider.removeUpdateListener(::handleUpdate)
    }

    companion object {
        
        @Volatile
        private var instance: TelegramRepository? = null

        fun getInstance(): TelegramRepository {
            return instance ?: synchronized(this) {
                instance ?: TelegramRepository().also { instance = it }
            }
        }
    }
}
