package com.example.bustrackingapp.feature_tracking.data.repository

import com.example.bustrackingapp.feature_tracking.data.local.TrackingBufferStore
import com.example.bustrackingapp.feature_tracking.data.remote.api.TrackingApiService
import com.example.bustrackingapp.feature_tracking.domain.model.TrackingPacket
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor(
    private val trackingApiService: TrackingApiService,
    private val trackingBufferStore: TrackingBufferStore,
) {

    /**
     * Attempt to transmit a single packet. If the network is down, buffer it locally.
     * After a successful send, opportunistically drain the offline backlog.
     */
    suspend fun transmit(packet: TrackingPacket): Boolean {
        val delivered = sendWithRetry(packet)
        if (!delivered) {
            trackingBufferStore.add(packet)
            return false
        }
        syncBufferedPackets()
        return true
    }

    /**
     * Drain the offline buffer in capped batches to prevent API overload.
     * Each batch is at most [SYNC_BATCH_SIZE] packets.
     */
    suspend fun syncBufferedPackets() {
        val queue = trackingBufferStore.getAll()
        if (queue.isEmpty()) return

        // Send in capped batches rather than the full queue at once
        val batch = queue.take(SYNC_BATCH_SIZE)
        val response = runCatching {
            trackingApiService.pushTrackingPacketBulk(batch)
        }.getOrNull()

        if (response?.isSuccessful == true) {
            trackingBufferStore.removeFirst(batch.size)
        }
    }

    /** Returns the current offline buffer size (Room-backed, suspend). */
    suspend fun getBufferedCount(): Int = trackingBufferStore.size()

    /**
     * Retry with exponential backoff instead of a flat 10s wait.
     * Delays: 2s → 4s → 8s (3 retries, capped to avoid long blocking).
     */
    private suspend fun sendWithRetry(packet: TrackingPacket): Boolean {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            val response = runCatching {
                trackingApiService.pushTrackingPacket(packet)
            }.getOrNull()

            if (response?.isSuccessful == true) return true

            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                val backoffMs = INITIAL_BACKOFF_MS * (1L shl attempt)  // 2s, 4s, 8s
                delay(backoffMs.coerceAtMost(MAX_BACKOFF_MS))
            }
        }
        return false
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 8_000L
        private const val SYNC_BATCH_SIZE = 50
    }
}
