package com.azurlize.team.ui.contact

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azurlize.team.data.local.AppDatabase
import com.azurlize.team.data.local.FavoriteContact
import com.azurlize.team.data.local.PinnedContact
import com.azurlize.team.telegram.repository.TelegramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelegramRepository.getInstance()
    private val localDao = AppDatabase.getDatabase(application).localDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TdApi.User>>(emptyList())
    val searchResults: StateFlow<List<TdApi.User>> = _searchResults.asStateFlow()

    val contacts: StateFlow<List<TdApi.User>> = repository.contacts

    val favoriteIds: StateFlow<Set<Long>> = localDao.getFavoriteContacts()
        .map { it.map { fav -> fav.userId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val pinnedIds: StateFlow<Set<Long>> = localDao.getPinnedContacts()
        .map { it.map { pin -> pin.userId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onSearch(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            repository.searchContacts(query) { results ->
                _searchResults.value = results
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun getFile(fileId: Int) = repository.getFile(fileId)

    fun createChat(userId: Long, callback: (TdApi.Chat?) -> Unit) {
        repository.createPrivateChat(userId, callback)
    }

    fun toggleFavorite(userId: Long) {
        viewModelScope.launch {
            if (favoriteIds.value.contains(userId)) {
                localDao.removeFavorite(FavoriteContact(userId))
            } else {
                localDao.addFavorite(FavoriteContact(userId))
            }
        }
    }

    fun togglePinned(userId: Long) {
        viewModelScope.launch {
            if (pinnedIds.value.contains(userId)) {
                localDao.removePinned(PinnedContact(userId))
            } else {
                localDao.addPinned(PinnedContact(userId))
            }
        }
    }

    fun getUserFullInfo(userId: Long, callback: (TdApi.UserFullInfo?) -> Unit) {
        repository.getUserFullInfo(userId, callback)
    }
}
