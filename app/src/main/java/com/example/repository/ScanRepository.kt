package com.example.repository

import com.example.data.dao.ScanHistoryDao
import com.example.data.entity.ScanHistoryEntity
import com.example.data.entity.ScannedDeviceEntity
import com.example.model.DeviceModel
import com.example.model.NetworkInfoModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanRepository(private val dao: ScanHistoryDao) {

    val allHistory: Flow<List<ScanHistoryEntity>> = dao.getAllHistory()

    fun getDevicesForHistory(historyId: Long): Flow<List<DeviceModel>> {
        return dao.getDevicesForHistory(historyId).map { list ->
            list.map { entity ->
                val ports = if (entity.openPortsStr.isBlank()) {
                    emptyList()
                } else {
                    entity.openPortsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                }
                DeviceModel(
                    ip = entity.ip,
                    mac = entity.mac,
                    hostname = entity.hostname,
                    vendor = entity.vendor,
                    responseTimeMs = entity.responseTimeMs,
                    isOnline = entity.isOnline,
                    openPorts = ports,
                    networkInterface = entity.networkInterface,
                    firstSeen = entity.firstSeen,
                    lastSeen = entity.lastSeen
                )
            }
        }
    }

    suspend fun saveScanSession(
        networkInfo: NetworkInfoModel,
        devices: List<DeviceModel>,
        durationSeconds: Long
    ): Long {
        val onlineCount = devices.count { it.isOnline }
        val offlineCount = devices.size - onlineCount

        val historyEntity = ScanHistoryEntity(
            timestamp = System.currentTimeMillis(),
            networkName = networkInfo.ssid,
            subnetCidr = networkInfo.cidr,
            deviceCount = devices.size,
            onlineCount = onlineCount,
            offlineCount = offlineCount,
            durationSeconds = durationSeconds
        )

        val deviceEntities = devices.map { d ->
            ScannedDeviceEntity(
                historyId = 0L,
                ip = d.ip,
                mac = d.mac,
                hostname = d.hostname,
                vendor = d.vendor,
                responseTimeMs = d.responseTimeMs,
                isOnline = d.isOnline,
                openPortsStr = d.openPorts.joinToString(","),
                networkInterface = d.networkInterface,
                firstSeen = d.firstSeen,
                lastSeen = d.lastSeen
            )
        }

        return dao.insertScanSession(historyEntity, deviceEntities)
    }

    suspend fun deleteHistory(id: Long) {
        dao.deleteHistoryWithDevices(id)
    }

    suspend fun clearAllHistory() {
        dao.clearAll()
    }
}
