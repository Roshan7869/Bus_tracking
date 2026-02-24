package com.example.bustrackingapp.feature_tracking.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bustrackingapp.feature_tracking.domain.model.ConfidenceLevel

/**
 * Room entity representing a single cached [TrackingPacket] waiting to be
 * synced when network connectivity is restored.
 *
 * Using individual columns (not a single JSON blob) means each insert/delete
 * is O(1) and never blocks the main thread with large serialisation work.
 */
@Entity(tableName = "tracking_buffer")
data class TrackingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val busId: String,
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmph: Float,
    val headingDegree: Float,
    val accuracyMeters: Float,
    val locationSource: String,
    val satelliteCount: Int?,
    val timestampUnix: Long,
    val batteryPercentage: Int,
    val networkType: String,
    val confidenceFlag: String?,   // stored as enum name string
)
