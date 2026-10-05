package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PingResult
import com.example.network.scanner.PingEngine
import com.example.utils.IpUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PingViewModel(private val pingEngine: PingEngine = PingEngine()) : ViewModel() {

    private val _targetIp = MutableStateFlow("")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    private val _pingResult = MutableStateFlow<PingResult?>(null)
    val pingResult: StateFlow<PingResult?> = _pingResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pingJob: Job? = null

    fun setTargetIp(ip: String) {
        _targetIp.value = ip
    }

    fun startPing(ip: String? = null, packetCount: Int = 4) {
        val target = (ip ?: _targetIp.value).trim()
        if (target.isBlank()) {
            _errorMessage.value = "Masukkan alamat IP target."
            return
        }
        if (!IpUtils.isValidIpv4(target)) {
            _errorMessage.value = "Format IP '$target' tidak valid."
            return
        }

        _targetIp.value = target
        _errorMessage.value = null
        stopPing()

        pingJob = viewModelScope.launch {
            pingEngine.executePing(target, packetCount).collect { result ->
                _pingResult.value = result
            }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        pingJob = null
        _pingResult.value = _pingResult.value?.copy(isPinging = false)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
    }
}
