package com.example.model

data class DeviceModel(
    val ip: String,
    val mac: String = "Tidak tersedia pada perangkat/versi Android ini",
    val hostname: String = "Unknown Host",
    val vendor: String = "Unknown",
    val responseTimeMs: Long = 0L,
    val isOnline: Boolean = true,
    val openPorts: List<Int> = emptyList(),
    val networkInterface: String = "wlan0",
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)
