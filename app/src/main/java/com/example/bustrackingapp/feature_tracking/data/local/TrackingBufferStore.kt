package com.example.bustrackingapp.feature_tracking.data.local

import android.content.Context
import com.example.bustrackingapp.core.util.GsonUtil
import com.example.bustrackingapp.feature_tracking.domain.model.TrackingPacket
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingBufferStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun add(packet: TrackingPacket) {
        val queue = getAll().toMutableList()
        if (queue.size >= MAX_RECORDS) {
            queue.removeFirst()
        }
        queue.add(packet)
        save(queue)
    }

    fun getAll(): List<TrackingPacket> {
        val json = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return GsonUtil.gson.fromJson(json, Array<TrackingPacket>::class.java)?.toList().orEmpty()
    }


    fun size(): Int = getAll().size

    fun removeFirst(count: Int) {
        if (count <= 0) return
        val queue = getAll().toMutableList()
        repeat(count.coerceAtMost(queue.size)) {
            queue.removeFirst()
        }
        save(queue)
    }

    private fun save(queue: List<TrackingPacket>) {
        prefs.edit().putString(KEY_QUEUE, GsonUtil.gson.toJson(queue)).apply()
    }

    companion object {
        private const val PREF_NAME = "tracking_buffer"
        private const val KEY_QUEUE = "tracking_queue"
        private const val MAX_RECORDS = 500
    }
}
