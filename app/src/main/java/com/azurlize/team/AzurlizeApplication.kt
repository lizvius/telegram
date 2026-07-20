package com.azurlize.team

import android.app.Application
import android.util.Log
import com.azurlize.team.telegram.client.TelegramClientProvider

class AzurlizeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("AzurlizeApplication", "Application onCreate - Initializing TDLib")
        TelegramClientProvider.initialize(this)
    }
}
