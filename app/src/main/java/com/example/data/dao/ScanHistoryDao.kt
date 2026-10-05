package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.entity.ScanHistoryEntity
import com.example.data.entity.ScannedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scanned_devices WHERE historyId = :historyId ORDER BY id ASC")
    fun getDevicesForHistory(historyId: Long): Flow<List<ScannedDeviceEntity>>

    @Query("SELECT * FROM scanned_devices WHERE historyId = :historyId ORDER BY id ASC")
    suspend fun getDevicesForHistoryDirect(historyId: Long): List<ScannedDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ScanHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<ScannedDeviceEntity>)

    @Transaction
    suspend fun insertScanSession(history: ScanHistoryEntity, devices: List<ScannedDeviceEntity>): Long {
        val historyId = insertHistory(history)
        val linkedDevices = devices.map { it.copy(historyId = historyId) }
        insertDevices(linkedDevices)
        return historyId
    }

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM scanned_devices WHERE historyId = :historyId")
    suspend fun deleteDevicesForHistory(historyId: Long)

    @Transaction
    suspend fun deleteHistoryWithDevices(id: Long) {
        deleteDevicesForHistory(id)
        deleteHistory(id)
    }

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllHistory()

    @Query("DELETE FROM scanned_devices")
    suspend fun deleteAllDevices()

    @Transaction
    suspend fun clearAll() {
        deleteAllDevices()
        deleteAllHistory()
    }
}
