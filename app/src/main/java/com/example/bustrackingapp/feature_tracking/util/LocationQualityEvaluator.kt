package com.example.bustrackingapp.feature_tracking.util

import android.location.Location
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationQualityEvaluator @Inject constructor() {

    fun getLocationSource(location: Location): String {
        return if (location.provider.equals("network", ignoreCase = true)) {
            "CELL_TOWER"
        } else {
            "GNSS"
        }
    }

    fun shouldUseCellTowerFallback(location: Location, satelliteCount: Int?): Boolean {
        return location.accuracy > 30f || (satelliteCount ?: 0) < 4
    }

    fun confidenceFlag(location: Location, satelliteCount: Int?): String? {
        return if (shouldUseCellTowerFallback(location, satelliteCount)) "LOW_ACCURACY_MODE" else null
    }
}
