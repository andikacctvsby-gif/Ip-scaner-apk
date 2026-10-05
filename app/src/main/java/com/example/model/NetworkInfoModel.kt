package com.example.model

data class NetworkInfoModel(
    val ssid: String = "Tidak Terhubung",
    val ipAddress: String = "0.0.0.0",
    val subnetMask: String = "255.255.255.0",
    val gateway: String = "0.0.0.0",
    val dnsServers: List<String> = emptyList(),
    val interfaceName: String = "wlan0",
    val connectionType: String = "Wi-Fi",
    val cidr: String = "0.0.0.0/24",
    val rangeStart: String = "0.0.0.0",
    val rangeEnd: String = "0.0.0.0",
    val totalHosts: Int = 0,
    val isConnected: Boolean = false
)
