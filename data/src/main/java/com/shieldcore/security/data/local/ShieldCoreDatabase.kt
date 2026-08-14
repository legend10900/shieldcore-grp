package com.shieldcore.security.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shieldcore.security.data.local.dao.ScanDao
import com.shieldcore.security.data.local.entity.ScanReportEntity

@Database(
    entities = [ScanReportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ShieldCoreDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}
