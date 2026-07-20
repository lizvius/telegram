package com.azurlize.team.telegram.client

import android.content.Context
import android.util.Log
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

object TelegramClientProvider {
    private const val TAG = "TelegramClientProvider"
    
    private var client: Client? = null
    private var currentAuthState: TdApi.AuthorizationState? = null
    
    private val authStateListeners = mutableListOf<(TdApi.AuthorizationState) -> Unit>()
    private val updateListeners = mutableListOf<(TdApi.Object) -> Unit>()

    fun initialize(context: Context): Client {
        if (client != null) return client!!

        val databaseDir = File(context.filesDir, "tdlib_db").absolutePath
        val filesDir = File(context.filesDir, "tdlib_files").absolutePath

        // Create update handler
        val updateHandler = Client.ResultHandler { objectResult ->
            // Notify generic update listeners
            notifyUpdateListeners(objectResult)

            if (objectResult is TdApi.UpdateAuthorizationState) {
                val state = objectResult.authorizationState
                Log.d(TAG, "Authorization State Updated: ${state.javaClass.simpleName}")
                currentAuthState = state
                
                // Automatically handle TDLib initialization state transitions
                when (state) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        val setParameters = TdApi.SetTdlibParameters(
                            false,                                        // useTestDc
                            databaseDir,                                  // databaseDirectory
                            filesDir,                                     // filesDirectory
                            ByteArray(0),                                 // databaseEncryptionKey
                            true,                                         // useFileDatabase
                            true,                                         // useChatInfoDatabase
                            true,                                         // useMessageDatabase
                            false,                                        // useSecretChats
                            com.azurlize.team.core.TelegramConfig.API_ID, // apiId
                            com.azurlize.team.core.TelegramConfig.API_HASH, // apiHash
                            "en",                                         // systemLanguageCode
                            android.os.Build.MODEL,                       // deviceModel
                            android.os.Build.VERSION.RELEASE,             // systemVersion
                            "1.0.0"                                       // applicationVersion
                        )
                        client?.send(setParameters, { result ->
                            if (result is TdApi.Error) {
                                Log.e(TAG, "Failed to set TDLib parameters: ${result.message}")
                            } else {
                                Log.d(TAG, "TDLib parameters set successfully")
                            }
                        })
                    }
                }
                
                notifyListeners(state)
            }
        }

        val updateExceptionHandler = Client.ExceptionHandler { throwable ->
            Log.e(TAG, "TDLib update exception", throwable)
        }

        val defaultExceptionHandler = Client.ExceptionHandler { throwable ->
            Log.e(TAG, "TDLib default exception", throwable)
        }

        client = Client.create(updateHandler, updateExceptionHandler, defaultExceptionHandler)

        return client!!
    }

    fun getClient(): Client {
        return client ?: throw IllegalStateException("TelegramClientProvider is not initialized. Call initialize() first.")
    }

    fun getCurrentState(): TdApi.AuthorizationState {
        return currentAuthState ?: TdApi.AuthorizationStateWaitTdlibParameters()
    }

    fun addStateListener(listener: (TdApi.AuthorizationState) -> Unit) {
        authStateListeners.add(listener)
        currentAuthState?.let { state ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                listener(state)
            }
        }
    }

    fun removeStateListener(listener: (TdApi.AuthorizationState) -> Unit) {
        authStateListeners.remove(listener)
    }

    fun addUpdateListener(listener: (TdApi.Object) -> Unit) {
        updateListeners.add(listener)
    }

    fun removeUpdateListener(listener: (TdApi.Object) -> Unit) {
        updateListeners.remove(listener)
    }

    private fun notifyUpdateListeners(update: TdApi.Object) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val listenersCopy = ArrayList(updateListeners)
            listenersCopy.forEach { it(update) }
        }
    }

    private fun notifyListeners(state: TdApi.AuthorizationState) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val listenersCopy = ArrayList(authStateListeners)
            listenersCopy.forEach { it(state) }
        }
    }
}
