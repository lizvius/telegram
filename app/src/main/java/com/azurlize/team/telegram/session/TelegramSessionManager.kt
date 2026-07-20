package com.azurlize.team.telegram.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.azurlize.team.telegram.client.TelegramClientProvider
import org.drinkless.tdlib.TdApi
import java.io.File

class TelegramSessionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "azurlize_telegram_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_USERNAME = "username"
    }

    fun saveSession(user: TdApi.User) {
        val username = user.usernames?.activeUsernames?.firstOrNull() ?: ""
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.id)
            putString(KEY_FIRST_NAME, user.firstName)
            putString(KEY_LAST_NAME, user.lastName)
            putString(KEY_PHONE_NUMBER, user.phoneNumber)
            putString(KEY_USERNAME, username)
            apply()
        }
        Log.d("TelegramSessionManager", "Session saved for User ID: ${user.id}")
    }

    fun restoreSession(): TdApi.User? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val id = prefs.getLong(KEY_USER_ID, 0)
        val firstName = prefs.getString(KEY_FIRST_NAME, "") ?: ""
        val lastName = prefs.getString(KEY_LAST_NAME, "") ?: ""
        val phoneNumber = prefs.getString(KEY_PHONE_NUMBER, "") ?: ""
        val username = prefs.getString(KEY_USERNAME, "") ?: ""

        return TdApi.User().apply {
            this.id = id
            this.firstName = firstName
            this.lastName = lastName
            this.phoneNumber = phoneNumber
            this.usernames = TdApi.Usernames().apply {
                this.activeUsernames = if (username.isNotEmpty()) arrayOf(username) else emptyArray()
            }
        }
    }

    fun logout(callback: (Boolean) -> Unit) {
        prefs.edit().clear().apply()
        
        try {
            val client = TelegramClientProvider.getClient()
            client.send(TdApi.LogOut(), { result ->
                if (result is TdApi.Ok) {
                    Log.d("TelegramSessionManager", "Logged out from TDLib successfully")
                    clearDatabase()
                    callback(true)
                } else {
                    Log.e("TelegramSessionManager", "Failed to log out from TDLib: $result")
                    callback(false)
                }
            })
        } catch (e: Exception) {
            Log.e("TelegramSessionManager", "Error on logging out", e)
            callback(false)
        }
    }

    fun clearDatabase() {
        try {
            val databaseDir = File(context.filesDir, "tdlib_db")
            if (databaseDir.exists()) {
                databaseDir.deleteRecursively()
            }
            val filesDir = File(context.filesDir, "tdlib_files")
            if (filesDir.exists()) {
                filesDir.deleteRecursively()
            }
            Log.d("TelegramSessionManager", "TDLib cache database cleared")
        } catch (e: Exception) {
            Log.e("TelegramSessionManager", "Error clearing TDLib database", e)
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && 
                TelegramClientProvider.getCurrentState() is TdApi.AuthorizationStateReady
    }

    fun authorizationState(): TdApi.AuthorizationState {
        return TelegramClientProvider.getCurrentState()
    }
}
