package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ScanHistoryDao
import com.example.data.entity.ScanHistoryEntity
import com.example.data.entity.ScannedDeviceEntity

@Database(
    entities = [ScanHistoryEntity::class, ScannedDeviceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ip_scanner_pro.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
