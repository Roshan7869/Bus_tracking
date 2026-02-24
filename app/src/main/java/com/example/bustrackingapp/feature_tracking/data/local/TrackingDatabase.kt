package com.example.bustrackingapp.feature_tracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackingLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun trackingBufferDao(): TrackingBufferDao
}
