package com.example.bustrackingapp.feature_tracking.domain.model

enum class ConfidenceLevel {
    LOW_ACCURACY_MODE,
    NO_LOCATION_UPDATE,
    LOW_BATTERY,
    AUTO_STOPPED
}

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
    val confidenceFlag: ConfidenceLevel? = null,
)
