package com.example

import android.app.Application
import com.example.data.SettingsRepository
import com.example.data.TransferDatabase
import com.example.data.TransferRepository

class QuickShareApp : Application() {
    val database by lazy { TransferDatabase.getDatabase(this) }
    val transferRepository by lazy { TransferRepository(database.transferHistoryDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
