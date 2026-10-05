package com.example.model

data class PingResult(
    val targetIp: String,
    val packetsTransmitted: Int = 0,
    val packetsReceived: Int = 0,
    val packetsLost: Int = 0,
    val minLatencyMs: Long = 0L,
    val maxLatencyMs: Long = 0L,
    val avgLatencyMs: Long = 0L,
    val logs: List<String> = emptyList(),
    val isPinging: Boolean = false
)

data class PortResult(
    val port: Int,
    val isOpen: Boolean,
    val serviceName: String,
    val responseTimeMs: Long = 0L
)

enum class ScanFilter(val label: String) {
    ALL("Semua"),
    ONLINE("Online"),
    OFFLINE("Offline")
}

enum class ScanSort(val label: String) {
    IP("IP Address"),
    RESPONSE_TIME("Response Time"),
    HOSTNAME("Hostname")
}
