package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DeviceModel
import com.example.model.NetworkInfoModel
import com.example.model.ScanFilter
import com.example.model.ScanSort
import com.example.network.NetworkManager
import com.example.network.scanner.IpScannerEngine
import com.example.network.scanner.ScanProgressEvent
import com.example.repository.ScanRepository
import com.example.repository.SettingsRepository
import com.example.utils.IpUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val networkInfo: NetworkInfoModel = NetworkInfoModel(),
    val isScanning: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val currentScannedIp: String = "",
    val statusMessage: String = "Siap untuk memindai",
    val errorMessage: String? = null,
    val scanDurationSeconds: Long = 0L,
    val lastScanSavedId: Long? = null
)

class ScannerViewModel(
    private val networkManager: NetworkManager,
    private val scannerEngine: IpScannerEngine,
    private val scanRepository: ScanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<DeviceModel>>(emptyList())
    val rawScannedDevices: StateFlow<List<DeviceModel>> = _scannedDevices.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(ScanFilter.ALL)
    val filter: StateFlow<ScanFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(ScanSort.IP)
    val sort: StateFlow<ScanSort> = _sort.asStateFlow()

    private var scanJob: Job? = null
    private var scanStartTime = 0L

    val filteredAndSortedDevices: StateFlow<List<DeviceModel>> = combine(
        _scannedDevices,
        _searchQuery,
        _filter,
        _sort
    ) { devices, query, filterMode, sortMode ->
        devices
            .asSequence()
            .filter { d ->
                when (filterMode) {
                    ScanFilter.ALL -> true
                    ScanFilter.ONLINE -> d.isOnline
                    ScanFilter.OFFLINE -> !d.isOnline
                }
            }
            .filter { d ->
                if (query.isBlank()) true
                else {
                    val q = query.trim().lowercase()
                    d.ip.lowercase().contains(q) ||
                    d.hostname.lowercase().contains(q) ||
                    d.mac.lowercase().contains(q) ||
                    d.vendor.lowercase().contains(q)
                }
            }
            .sortedWith { d1, d2 ->
                when (sortMode) {
                    ScanSort.IP -> IpUtils.ipToLong(d1.ip).compareTo(IpUtils.ipToLong(d2.ip))
                    ScanSort.RESPONSE_TIME -> d1.responseTimeMs.compareTo(d2.responseTimeMs)
                    ScanSort.HOSTNAME -> d1.hostname.compareTo(d2.hostname, ignoreCase = true)
                }
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshNetworkInfo()
    }

    fun refreshNetworkInfo() {
        viewModelScope.launch {
            val info = networkManager.getNetworkInfo()
            _uiState.value = _uiState.value.copy(
                networkInfo = info,
                errorMessage = if (!info.isConnected) "Perangkat tidak terhubung ke jaringan lokal." else null
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ScanFilter) {
        _filter.value = filter
    }

    fun setSort(sort: ScanSort) {
        _sort.value = sort
    }

    fun startAutoScan() {
        val currentInfo = networkManager.getNetworkInfo()
        _uiState.value = _uiState.value.copy(networkInfo = currentInfo)

        if (!currentInfo.isConnected && currentInfo.ipAddress == "0.0.0.0") {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Gagal memulai scan: Wi-Fi atau Ethernet tidak terhubung."
            )
            return
        }

        val ipList = IpUtils.generateIpList(currentInfo.rangeStart, currentInfo.rangeEnd)
        if (ipList.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Subnet tidak dapat ditentukan secara otomatis."
            )
            return
        }

        executeScan(ipList, "Memindai subnet ${currentInfo.cidr}...")
    }

    fun startCustomRangeScan(startIp: String, endIp: String) {
        if (!IpUtils.isValidIpv4(startIp)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Format IP Start '$startIp' tidak valid.")
            return
        }
        if (!IpUtils.isValidIpv4(endIp)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Format IP End '$endIp' tidak valid.")
            return
        }
        if (IpUtils.ipToLong(startIp) > IpUtils.ipToLong(endIp)) {
            _uiState.value = _uiState.value.copy(errorMessage = "IP Start tidak boleh lebih besar dari IP End.")
            return
        }

        val ipList = IpUtils.generateIpList(startIp, endIp, maxLimit = 1024)
        if (ipList.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Rentang IP tidak menghasilkan alamat yang valid.")
            return
        }

        executeScan(ipList, "Memindai rentang $startIp - $endIp (${ipList.size} host)...")
    }

    fun startCidrScan(cidr: String) {
        if (!IpUtils.isValidCidr(cidr)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Format CIDR '$cidr' tidak valid (Contoh: 192.168.1.0/24)."
            )
            return
        }

        val ipList = IpUtils.generateCidrIpList(cidr, maxLimit = 1024)
        if (ipList.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "CIDR tidak dapat menghasilkan alamat host.")
            return
        }

        executeScan(ipList, "Memindai CIDR $cidr (${ipList.size} host)...")
    }

    private fun executeScan(ipList: List<String>, initialStatus: String) {
        stopScan()

        _scannedDevices.value = emptyList()
        scanStartTime = System.currentTimeMillis()

        val settings = settingsRepository.settings.value

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            progressCurrent = 0,
            progressTotal = ipList.size,
            currentScannedIp = "",
            statusMessage = initialStatus,
            errorMessage = null,
            scanDurationSeconds = 0L
        )

        scanJob = viewModelScope.launch {
            scannerEngine.scanIps(
                ipList = ipList,
                concurrencyLimit = settings.concurrencyLimit,
                timeoutMs = settings.scanTimeoutMs
            ).collect { event ->
                when (event) {
                    is ScanProgressEvent.ProgressUpdate -> {
                        _uiState.value = _uiState.value.copy(
                            progressCurrent = event.current,
                            progressTotal = event.total,
                            currentScannedIp = event.currentIp,
                            statusMessage = "Scanning ${event.current} / ${event.total}"
                        )
                    }
                    is ScanProgressEvent.DeviceFound -> {
                        val currentList = _scannedDevices.value.toMutableList()
                        val existingIndex = currentList.indexOfFirst { it.ip == event.device.ip }
                        if (existingIndex >= 0) {
                            currentList[existingIndex] = event.device
                        } else {
                            currentList.add(event.device)
                        }
                        _scannedDevices.value = currentList
                    }
                    is ScanProgressEvent.Completed -> {
                        val duration = (System.currentTimeMillis() - scanStartTime) / 1000
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            progressCurrent = event.totalScanned,
                            statusMessage = "Scan selesai. Ditemukan ${event.onlineCount} perangkat online.",
                            scanDurationSeconds = duration
                        )
                        saveScanResult(duration)
                    }
                    is ScanProgressEvent.Cancelled -> {
                        val duration = (System.currentTimeMillis() - scanStartTime) / 1000
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            statusMessage = "Scan dihentikan oleh pengguna pada host ${event.current}.",
                            scanDurationSeconds = duration
                        )
                    }
                    is ScanProgressEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            errorMessage = event.message,
                            statusMessage = "Scan gagal."
                        )
                    }
                }
            }
        }
    }

    fun stopScan() {
        if (scanJob?.isActive == true) {
            scanJob?.cancel()
            scanJob = null
            val duration = (System.currentTimeMillis() - scanStartTime) / 1000
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                statusMessage = "Pemindaian dihentikan.",
                scanDurationSeconds = duration
            )
        }
    }

    private fun saveScanResult(durationSeconds: Long) {
        val settings = settingsRepository.settings.value
        if (!settings.autoSaveHistory) return
        val devices = _scannedDevices.value
        if (devices.isEmpty()) return

        viewModelScope.launch {
            try {
                val id = scanRepository.saveScanSession(
                    networkInfo = _uiState.value.networkInfo,
                    devices = devices,
                    durationSeconds = durationSeconds
                )
                _uiState.value = _uiState.value.copy(lastScanSavedId = id)
            } catch (_: Exception) {
                // Ignore storage error
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
