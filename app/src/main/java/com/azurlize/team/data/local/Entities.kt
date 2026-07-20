package com.azurlize.team.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_contacts")
data class FavoriteContact(
    @PrimaryKey val userId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pinned_contacts")
data class PinnedContact(
    @PrimaryKey val userId: Long,
    val pinnedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_searches")
data class RecentSearch(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
