package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PortResult
import com.example.network.scanner.PortScanEvent
import com.example.network.scanner.PortScannerEngine
import com.example.repository.SettingsRepository
import com.example.utils.IpUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PortScannerUiState(
    val targetIp: String = "",
    val isQuickScan: Boolean = true,
    val customPortInput: String = "80,443,8080,22,21,53,3389",
    val isScanning: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val currentPort: Int = 0,
    val openPortsCount: Int = 0,
    val statusMessage: String = "Pilih tipe pemindaian port",
    val errorMessage: String? = null
)

class PortScannerViewModel(
    private val portScannerEngine: PortScannerEngine = PortScannerEngine(),
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortScannerUiState())
    val uiState: StateFlow<PortScannerUiState> = _uiState.asStateFlow()

    private val _portResults = MutableStateFlow<List<PortResult>>(emptyList())
    val portResults: StateFlow<List<PortResult>> = _portResults.asStateFlow()

    private var scanJob: Job? = null

    fun setTargetIp(ip: String) {
        _uiState.value = _uiState.value.copy(targetIp = ip)
    }

    fun setIsQuickScan(isQuick: Boolean) {
        _uiState.value = _uiState.value.copy(isQuickScan = isQuick)
    }

    fun setCustomPortInput(input: String) {
        _uiState.value = _uiState.value.copy(customPortInput = input)
    }

    fun startScan(ip: String? = null) {
        val target = (ip ?: _uiState.value.targetIp).trim()
        if (target.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Masukkan IP target untuk port scanner.")
            return
        }
        if (!IpUtils.isValidIpv4(target)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Format IP '$target' tidak valid.")
            return
        }

        val portsToScan = if (_uiState.value.isQuickScan) {
            PortScannerEngine.COMMON_PORTS.map { it.first }
        } else {
            val parsed = PortScannerEngine.parseCustomPorts(_uiState.value.customPortInput)
            if (parsed.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Daftar port kustom tidak valid. Contoh: 80,443,8080 atau 8000-8010"
                )
                return
            }
            parsed
        }

        stopScan()
        _portResults.value = emptyList()

        _uiState.value = _uiState.value.copy(
            targetIp = target,
            isScanning = true,
            progressCurrent = 0,
            progressTotal = portsToScan.size,
            currentPort = 0,
            openPortsCount = 0,
            statusMessage = "Memindai ${portsToScan.size} port pada $target...",
            errorMessage = null
        )

        val timeout = settingsRepository?.settings?.value?.scanTimeoutMs ?: 400

        scanJob = viewModelScope.launch {
            portScannerEngine.scanPorts(
                targetIp = target,
                portsToScan = portsToScan,
                timeoutMs = timeout,
                concurrencyLimit = 10
            ).collect { event ->
                when (event) {
                    is PortScanEvent.Progress -> {
                        _uiState.value = _uiState.value.copy(
                            progressCurrent = event.current,
                            progressTotal = event.total,
                            currentPort = event.currentPort,
                            statusMessage = "Memeriksa port ${event.currentPort} (${event.current}/${event.total})"
                        )
                    }
                    is PortScanEvent.PortFound -> {
                        val currentList = _portResults.value.toMutableList()
                        currentList.add(event.result)
                        _portResults.value = currentList
                        if (event.result.isOpen) {
                            _uiState.value = _uiState.value.copy(
                                openPortsCount = _uiState.value.openPortsCount + 1
                            )
                        }
                    }
                    is PortScanEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            progressCurrent = event.totalScanned,
                            statusMessage = "Port scan selesai. ${event.openCount} port terbuka ditemukan."
                        )
                    }
                    is PortScanEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            errorMessage = event.message,
                            statusMessage = "Gagal memindai port."
                        )
                    }
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            statusMessage = "Port scan dihentikan."
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
