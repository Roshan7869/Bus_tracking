package com.example.bustrackingapp.feature_tracking.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TrackingBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val busId = prefs.getString(KEY_BUS_ID, null)
        val driverId = prefs.getString(KEY_DRIVER_ID, null)
        val trackingEnabled = prefs.getBoolean(KEY_TRACKING_ENABLED, false)

        if (trackingEnabled && !busId.isNullOrBlank() && !driverId.isNullOrBlank()) {
            BusTrackingForegroundService.start(context, busId, driverId)
        }
    }

    companion object {
        const val PREF_NAME = "tracking_runtime"
        const val KEY_TRACKING_ENABLED = "tracking_enabled"
        const val KEY_BUS_ID = "bus_id"
        const val KEY_DRIVER_ID = "driver_id"
    }
}
