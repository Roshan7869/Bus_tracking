package com.example.bustrackingapp.feature_tracking.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.bustrackingapp.MainActivity
import com.example.bustrackingapp.R
import com.example.bustrackingapp.feature_tracking.data.repository.TrackingRepository
import com.example.bustrackingapp.feature_tracking.domain.model.TrackingPacket
import com.example.bustrackingapp.feature_tracking.util.LocationQualityEvaluator
import com.example.bustrackingapp.feature_tracking.util.TrackingDeviceStateReader
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BusTrackingForegroundService : Service() {

    @Inject lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject lateinit var trackingRepository: TrackingRepository
    @Inject lateinit var trackingDeviceStateReader: TrackingDeviceStateReader
    @Inject lateinit var locationQualityEvaluator: LocationQualityEvaluator

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var busId: String = ""
    private var driverId: String = ""
    private var lastLocation: Location? = null
    private var lastLocationTimestamp: Long = 0L
    private var lowBatteryAlertSent = false

    private val runtimePrefs by lazy {
        getSharedPreferences(TrackingBootReceiver.PREF_NAME, Context.MODE_PRIVATE)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation = locationResult.lastLocation
            lastLocationTimestamp = System.currentTimeMillis()
            runtimePrefs.edit().putLong(KEY_LAST_LOCATION_TS, lastLocationTimestamp).apply()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTracking()
            ACTION_START -> {
                busId = intent.getStringExtra(EXTRA_BUS_ID).orEmpty()
                driverId = intent.getStringExtra(EXTRA_DRIVER_ID).orEmpty()
                if (busId.isBlank() || driverId.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, createNotification())
                acquireWakeLockIfNeeded()
                startLocationUpdates()
                startHeartbeat()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return

        heartbeatJob = serviceScope.launch {
            while (isActive) {
                acquireWakeLockIfNeeded()
                val now = System.currentTimeMillis()
                val networkType = trackingDeviceStateReader.getNetworkType()
                val batteryPercentage = trackingDeviceStateReader.getBatteryPercentage()

                var sentSuccessfully = false
                lastLocation?.let { location ->
                    val packet = createPacket(location, batteryPercentage, networkType)
                    sentSuccessfully = trackingRepository.transmit(packet)
                    emitOperationalAlerts(packet)
                }

                if (now - lastLocationTimestamp > NO_LOCATION_UPDATE_THRESHOLD_MS) {
                    sentSuccessfully = trackingRepository.transmit(
                        TrackingPacket(
                            busId = busId,
                            driverId = driverId,
                            latitude = 0.0,
                            longitude = 0.0,
                            speedKmph = 0f,
                            headingDegree = 0f,
                            accuracyMeters = 999f,
                            locationSource = "GNSS",
                            satelliteCount = null,
                            timestampUnix = now,
                            batteryPercentage = batteryPercentage,
                            networkType = networkType,
                            confidenceFlag = "NO_LOCATION_UPDATE",
                        ),
                    )
                }

                publishRuntimeStatus(now, batteryPercentage, networkType, sentSuccessfully)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun publishRuntimeStatus(
        now: Long,
        batteryPercentage: Int,
        networkType: String,
        sentSuccessfully: Boolean,
    ) {
        runtimePrefs.edit()
            .putLong(KEY_LAST_HEARTBEAT_TS, now)
            .putLong(KEY_LAST_SUCCESS_TS, if (sentSuccessfully) now else runtimePrefs.getLong(KEY_LAST_SUCCESS_TS, 0L))
            .putInt(KEY_BUFFERED_COUNT, trackingRepository.getBufferedCount())
            .putInt(KEY_BATTERY_PERCENT, batteryPercentage)
            .putString(KEY_NETWORK_TYPE, networkType)
            .apply()
    }

    private fun emitOperationalAlerts(packet: TrackingPacket) {
        if (packet.batteryPercentage <= LOW_BATTERY_ALERT_THRESHOLD && !lowBatteryAlertSent) {
            lowBatteryAlertSent = true
            trackingRepository.transmit(packet.copy(confidenceFlag = "LOW_BATTERY"))
        } else if (packet.batteryPercentage > LOW_BATTERY_ALERT_THRESHOLD) {
            lowBatteryAlertSent = false
        }
    }

    private fun createPacket(
        location: Location,
        batteryPercentage: Int,
        networkType: String,
    ): TrackingPacket {
        val satelliteCount: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) location.extras?.getInt("satellites") else null
        val confidenceFlag = locationQualityEvaluator.confidenceFlag(location, satelliteCount)
        return TrackingPacket(
            busId = busId,
            driverId = driverId,
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmph = location.speed * 3.6f,
            headingDegree = location.bearing,
            accuracyMeters = location.accuracy,
            locationSource = locationQualityEvaluator.getLocationSource(location),
            satelliteCount = satelliteCount,
            timestampUnix = System.currentTimeMillis(),
            batteryPercentage = batteryPercentage,
            networkType = networkType,
            confidenceFlag = confidenceFlag,
        )
    }

    private fun stopTracking() {
        heartbeatJob?.cancel()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        wakeLock?.takeIf { it.isHeld }?.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bus tracking active")
            .setContentText("Live location is being shared every 15 seconds")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bus Tracking",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLockIfNeeded() {
        val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BusTrackingWakeLock")
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ACTION_START = "bus_tracking_start"
        const val ACTION_STOP = "bus_tracking_stop"
        const val EXTRA_BUS_ID = "extra_bus_id"
        const val EXTRA_DRIVER_ID = "extra_driver_id"

        const val KEY_LAST_HEARTBEAT_TS = "tracking_last_heartbeat_ts"
        const val KEY_LAST_SUCCESS_TS = "tracking_last_success_ts"
        const val KEY_LAST_LOCATION_TS = "tracking_last_location_ts"
        const val KEY_BUFFERED_COUNT = "tracking_buffered_count"
        const val KEY_BATTERY_PERCENT = "tracking_battery_percent"
        const val KEY_NETWORK_TYPE = "tracking_network_type"

        private const val CHANNEL_ID = "bus_tracking_channel"
        private const val NOTIFICATION_ID = 404
        private const val LOCATION_UPDATE_INTERVAL_MS = 3_000L
        private const val FASTEST_UPDATE_INTERVAL_MS = 2_000L
        private const val MIN_DISPLACEMENT_METERS = 5f
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
        private const val NO_LOCATION_UPDATE_THRESHOLD_MS = 60_000L
        private const val LOW_BATTERY_ALERT_THRESHOLD = 20
        private const val WAKE_LOCK_TIMEOUT_MS = 20 * 60 * 1000L

        fun start(context: Context, busId: String, driverId: String) {
            val intent = Intent(context, BusTrackingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_BUS_ID, busId)
                putExtra(EXTRA_DRIVER_ID, driverId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BusTrackingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
