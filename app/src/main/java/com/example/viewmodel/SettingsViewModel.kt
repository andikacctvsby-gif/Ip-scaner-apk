package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.AppSettings
import com.example.repository.AppThemeMode
import com.example.repository.ScanRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setScanTimeout(timeoutMs: Int) {
        settingsRepository.setScanTimeout(timeoutMs)
    }

    fun setConcurrencyLimit(limit: Int) {
        settingsRepository.setConcurrencyLimit(limit)
    }

    fun setAutoSaveHistory(enabled: Boolean) {
        settingsRepository.setAutoSaveHistory(enabled)
    }

    fun setDefaultPorts(ports: String) {
        settingsRepository.setDefaultPorts(ports)
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            scanRepository.clearAllHistory()
            onComplete()
        }
    }
}
