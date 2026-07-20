package com.azurlize.team.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao {
    // Favorites
    @Query("SELECT * FROM favorite_contacts ORDER BY addedAt DESC")
    fun getFavoriteContacts(): Flow<List<FavoriteContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(contact: FavoriteContact)

    @Delete
    suspend fun removeFavorite(contact: FavoriteContact)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_contacts WHERE userId = :userId)")
    suspend fun isFavorite(userId: Long): Boolean

    // Pinned
    @Query("SELECT * FROM pinned_contacts ORDER BY pinnedAt DESC")
    fun getPinnedContacts(): Flow<List<PinnedContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPinned(contact: PinnedContact)

    @Delete
    suspend fun removePinned(contact: PinnedContact)

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_contacts WHERE userId = :userId)")
    suspend fun isPinned(userId: Long): Boolean

    // Recent Search
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentSearch(search: RecentSearch)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}

@Database(
    entities = [FavoriteContact::class, PinnedContact::class, RecentSearch::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
