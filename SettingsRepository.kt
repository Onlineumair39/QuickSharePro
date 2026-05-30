package com.example.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("quick_share_settings", Context.MODE_PRIVATE)

    private val _deviceName = MutableStateFlow(getSavedDeviceName())
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _downloadLocation = MutableStateFlow(getSavedDownloadLocation())
    val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()

    private val _darkThemeMode = MutableStateFlow(getSavedThemeMode()) // 0 = System, 1 = Light, 2 = Dark
    val darkThemeMode: StateFlow<Int> = _darkThemeMode.asStateFlow()

    private fun getSavedDeviceName(): String {
        val defaultName = "QuickShare_${Build.MODEL.replace(" ", "_").take(10)}_${Random.nextInt(1000, 9999)}"
        return prefs.getString("device_name", defaultName) ?: defaultName
    }

    private fun getSavedDownloadLocation(): String {
        return prefs.getString("download_location", "Downloads/QuickSharePro") ?: "Downloads/QuickSharePro"
    }

    private fun getSavedThemeMode(): Int {
        return prefs.getInt("theme_mode", 0) // default 0 (System)
    }

    fun updateDeviceName(name: String) {
        val cleanName = name.trim().ifEmpty { getSavedDeviceName() }
        prefs.edit().putString("device_name", cleanName).apply()
        _deviceName.value = cleanName
    }

    fun updateDownloadLocation(path: String) {
        val cleanPath = path.trim().ifEmpty { "Downloads/QuickSharePro" }
        prefs.edit().putString("download_location", cleanPath).apply()
        _downloadLocation.value = cleanPath
    }

    fun updateThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _darkThemeMode.value = mode
    }
}
