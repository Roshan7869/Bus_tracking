package com.example.bustrackingapp.feature_home.presentation.home_driver

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrackingapp.feature_tracking.service.BusTrackingForegroundService
import com.example.bustrackingapp.feature_tracking.service.TrackingBootReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeDriverViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    var uiState by mutableStateOf(HomeDriverUiState())
        private set

    private var statusJob: Job? = null

    init {
        loadPersistedRuntimeState()
        startStatusPolling()
    }

    fun onBusIdChange(value: String) {
        uiState = uiState.copy(busId = value)
    }

    fun onDriverIdChange(value: String) {
        uiState = uiState.copy(driverId = value)
    }

    fun startTracking(context: Context) {
        val busId = uiState.busId.trim()
        val driverId = uiState.driverId.trim()

        if (busId.isBlank() || driverId.isBlank()) {
            uiState = uiState.copy(error = "Bus ID and Driver ID are required")
            return
        }

        persistTrackingState(context, true, busId, driverId)
        BusTrackingForegroundService.start(context, busId, driverId)
        uiState = uiState.copy(isTracking = true, error = null)
    }

    fun stopTracking(context: Context) {
        persistTrackingState(context, false, uiState.busId, uiState.driverId)
        BusTrackingForegroundService.stop(context)
        uiState = uiState.copy(isTracking = false)
    }

    fun consumeError() {
        uiState = uiState.copy(error = null)
    }

    private fun startStatusPolling() {
        if (statusJob?.isActive == true) return
        statusJob = viewModelScope.launch {
            while (isActive) {
                loadPersistedRuntimeState()
                delay(5_000L)
            }
        }
    }

    private fun loadPersistedRuntimeState() {
        val prefs = appContext.getSharedPreferences(TrackingBootReceiver.PREF_NAME, Context.MODE_PRIVATE)
        uiState = uiState.copy(
            busId = if (uiState.busId.isBlank()) prefs.getString(TrackingBootReceiver.KEY_BUS_ID, "").orEmpty() else uiState.busId,
            driverId = if (uiState.driverId.isBlank()) prefs.getString(TrackingBootReceiver.KEY_DRIVER_ID, "").orEmpty() else uiState.driverId,
            isTracking = prefs.getBoolean(TrackingBootReceiver.KEY_TRACKING_ENABLED, false),
            lastHeartbeatTs = prefs.getLong(BusTrackingForegroundService.KEY_LAST_HEARTBEAT_TS, 0L),
            lastSuccessTs = prefs.getLong(BusTrackingForegroundService.KEY_LAST_SUCCESS_TS, 0L),
            bufferedCount = prefs.getInt(BusTrackingForegroundService.KEY_BUFFERED_COUNT, 0),
            batteryPercent = prefs.getInt(BusTrackingForegroundService.KEY_BATTERY_PERCENT, -1),
            networkType = prefs.getString(BusTrackingForegroundService.KEY_NETWORK_TYPE, "UNKNOWN").orEmpty(),
        )
    }

    private fun persistTrackingState(context: Context, enabled: Boolean, busId: String, driverId: String) {
        context.getSharedPreferences(TrackingBootReceiver.PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(TrackingBootReceiver.KEY_TRACKING_ENABLED, enabled)
            .putString(TrackingBootReceiver.KEY_BUS_ID, busId)
            .putString(TrackingBootReceiver.KEY_DRIVER_ID, driverId)
            .apply()
    }
}
