package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val networkName: String,
    val subnetCidr: String,
    val deviceCount: Int,
    val onlineCount: Int,
    val offlineCount: Int,
    val durationSeconds: Long
)

@Entity(tableName = "scanned_devices")
data class ScannedDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val historyId: Long,
    val ip: String,
    val mac: String,
    val hostname: String,
    val vendor: String,
    val responseTimeMs: Long,
    val isOnline: Boolean,
    val openPortsStr: String,
    val networkInterface: String,
    val firstSeen: Long,
    val lastSeen: Long
)
