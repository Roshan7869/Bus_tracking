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

    suspend fun transmit(packet: TrackingPacket): Boolean {
        val delivered = sendWithRetry(packet)
        if (!delivered) {
            trackingBufferStore.add(packet)
            return false
        }
        syncBufferedPackets()
        return true
    }

    suspend fun syncBufferedPackets() {
        val queue = trackingBufferStore.getAll()
        if (queue.isEmpty()) return

        val response = runCatching {
            trackingApiService.pushTrackingPacketBulk(queue)
        }.getOrNull()

        if (response?.isSuccessful == true) {
            trackingBufferStore.removeFirst(queue.size)
        }
    }

    fun getBufferedCount(): Int = trackingBufferStore.size()

    private suspend fun sendWithRetry(packet: TrackingPacket): Boolean {
        repeat(MAX_RETRY_ATTEMPTS) { index ->
            val response = runCatching {
                trackingApiService.pushTrackingPacket(packet)
            }.getOrNull()

            if (response?.isSuccessful == true) {
                return true
            }

            if (index < MAX_RETRY_ATTEMPTS - 1) {
                delay(RETRY_INTERVAL_MS)
            }
        }
        return false
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val RETRY_INTERVAL_MS = 10_000L
    }
}
