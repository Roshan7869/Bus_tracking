package com.example.bustrackingapp.feature_tracking.domain.model

data class TrackingPacket(
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
    val confidenceFlag: String? = null,
)
