package com.example.bustrackingapp.feature_home.presentation.home_driver

import com.example.bustrackingapp.core.domain.models.User

data class HomeDriverUiState(
    val user: User? = null,
    val busId: String = "",
    val driverId: String = "",
    val isTracking: Boolean = false,
    val lastHeartbeatTs: Long = 0L,
    val lastSuccessTs: Long = 0L,
    val bufferedCount: Int = 0,
    val batteryPercent: Int = -1,
    val networkType: String = "UNKNOWN",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    /** True when the device has no data connectivity at all. */
    val isOffline: Boolean get() = networkType.equals("NONE", ignoreCase = true)

    /** True when there are locally buffered packets waiting to be sent to the server. */
    val isSyncing: Boolean get() = !isOffline && bufferedCount > 0
}
