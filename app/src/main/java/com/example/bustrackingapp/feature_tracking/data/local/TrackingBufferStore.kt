package com.example.bustrackingapp.feature_tracking.data.local

import com.example.bustrackingapp.feature_tracking.domain.model.ConfidenceLevel
import com.example.bustrackingapp.feature_tracking.domain.model.TrackingPacket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline packet queue backed by Room (replaces the previous SharedPreferences JSON-blob
 * implementation). Every operation is O(1) and runs on a background thread via
 * suspend functions — the UI thread is never blocked.
 */
@Singleton
class TrackingBufferStore @Inject constructor(
    private val dao: TrackingBufferDao,
) {
    companion object {
        const val MAX_RECORDS = 500
    }

    /** Add a packet to the buffer, evicting the oldest if the cap is reached. */
    suspend fun add(packet: TrackingPacket) {
        if (dao.count() >= MAX_RECORDS) {
            dao.deleteOldestOne()   // FIFO eviction — keeps the newest data
        }
        dao.insert(packet.toEntity())
    }

    /** Return all buffered packets ordered chronologically (oldest first). */
    suspend fun getAll(): List<TrackingPacket> =
        dao.getAll().map { it.toPacket() }

    /** Remove the [count] oldest packets — called after a successful bulk sync. */
    suspend fun removeFirst(count: Int) {
        dao.deleteOldest(count)
    }

    /** Current size of the offline queue. */
    suspend fun size(): Int = dao.count()
}

// ─── Mapping helpers ──────────────────────────────────────────────────────────

private fun TrackingPacket.toEntity() = TrackingLogEntity(
    busId            = busId,
    driverId         = driverId,
    latitude         = latitude,
    longitude        = longitude,
    speedKmph        = speedKmph,
    headingDegree    = headingDegree,
    accuracyMeters   = accuracyMeters,
    locationSource   = locationSource,
    satelliteCount   = satelliteCount,
    timestampUnix    = timestampUnix,
    batteryPercentage = batteryPercentage,
    networkType      = networkType,
    confidenceFlag   = confidenceFlag?.name,
)

private fun TrackingLogEntity.toPacket() = TrackingPacket(
    busId            = busId,
    driverId         = driverId,
    latitude         = latitude,
    longitude        = longitude,
    speedKmph        = speedKmph,
    headingDegree    = headingDegree,
    accuracyMeters   = accuracyMeters,
    locationSource   = locationSource,
    satelliteCount   = satelliteCount,
    timestampUnix    = timestampUnix,
    batteryPercentage = batteryPercentage,
    networkType      = networkType,
    confidenceFlag   = confidenceFlag?.let { runCatching { ConfidenceLevel.valueOf(it) }.getOrNull() },
)
