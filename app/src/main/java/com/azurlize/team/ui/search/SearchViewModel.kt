package com.azurlize.team.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.data.local.AppDatabase
import com.azurlize.team.data.local.RecentSearch
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelegramRepository.getInstance()
    private val localDao = AppDatabase.getDatabase(application).localDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    val recentSearches = localDao.getRecentSearches()

    fun onSearch(query: String) {
        _query.value = query
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            // Search Chats
            repository.searchChats(query) { chats ->
                val chatResults = chats.map { SearchResult.ChatResult(it) }
                updateResults(chatResults)
            }
            // Search Contacts
            repository.searchContacts(query) { users ->
                val userResults = users.map { SearchResult.UserResult(it) }
                updateResults(userResults)
            }
        }
    }

    private fun updateResults(newResults: List<SearchResult>) {
        val current = _searchResults.value.toMutableList()
        current.addAll(newResults)
        _searchResults.value = current.distinctBy { it.id }.sortedBy { it.id }
    }

    fun addRecentSearch(query: String) {
        viewModelScope.launch {
            localDao.addRecentSearch(RecentSearch(query))
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            localDao.deleteRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            localDao.clearRecentSearches()
        }
    }

    fun getFile(fileId: Int) = repository.getFile(fileId)
}

sealed class SearchResult {
    abstract val id: String

    data class ChatResult(val chat: TdApi.Chat) : SearchResult() {
        override val id: String = "chat_${chat.id}"
    }

    data class UserResult(val user: TdApi.User) : SearchResult() {
        override val id: String = "user_${user.id}"
    }
}
