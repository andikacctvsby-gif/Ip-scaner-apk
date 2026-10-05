package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ScanHistoryEntity
import com.example.model.DeviceModel
import com.example.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ScanRepository) : ViewModel() {

    val historyList: StateFlow<List<ScanHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedHistory = MutableStateFlow<ScanHistoryEntity?>(null)
    val selectedHistory: StateFlow<ScanHistoryEntity?> = _selectedHistory.asStateFlow()

    private val _devicesForSelected = MutableStateFlow<List<DeviceModel>>(emptyList())
    val devicesForSelected: StateFlow<List<DeviceModel>> = _devicesForSelected.asStateFlow()

    fun selectHistory(item: ScanHistoryEntity) {
        _selectedHistory.value = item
        viewModelScope.launch {
            repository.getDevicesForHistory(item.id).collect { devices ->
                _devicesForSelected.value = devices
            }
        }
    }

    fun clearSelected() {
        _selectedHistory.value = null
        _devicesForSelected.value = emptyList()
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
            if (_selectedHistory.value?.id == id) {
                clearSelected()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            clearSelected()
        }
    }
}
