package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val scanTimeoutMs: Int = 350,
    val concurrencyLimit: Int = 20,
    val autoSaveHistory: Boolean = true,
    val defaultPorts: String = "80,443,8080,22,21,53,3389"
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ip_scanner_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val themeMode = try {
            AppThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
        val timeout = prefs.getInt("scan_timeout_ms", 350)
        val concurrency = prefs.getInt("concurrency_limit", 20)
        val autoSave = prefs.getBoolean("auto_save_history", true)
        val defaultPorts = prefs.getString("default_ports", "80,443,8080,22,21,53,3389") ?: "80,443,8080,22,21,53,3389"

        return AppSettings(
            themeMode = themeMode,
            scanTimeoutMs = timeout,
            concurrencyLimit = concurrency,
            autoSaveHistory = autoSave,
            defaultPorts = defaultPorts
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setScanTimeout(timeoutMs: Int) {
        prefs.edit().putInt("scan_timeout_ms", timeoutMs).apply()
        _settings.value = _settings.value.copy(scanTimeoutMs = timeoutMs)
    }

    fun setConcurrencyLimit(limit: Int) {
        prefs.edit().putInt("concurrency_limit", limit).apply()
        _settings.value = _settings.value.copy(concurrencyLimit = limit)
    }

    fun setAutoSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_history", enabled).apply()
        _settings.value = _settings.value.copy(autoSaveHistory = enabled)
    }

    fun setDefaultPorts(ports: String) {
        prefs.edit().putString("default_ports", ports).apply()
        _settings.value = _settings.value.copy(defaultPorts = ports)
    }
}
