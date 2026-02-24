package com.example.bustrackingapp.feature_tracking.data.remote.api

import com.example.bustrackingapp.feature_tracking.domain.model.TrackingPacket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TrackingApiService {

    @POST("tracking/location")
    suspend fun pushTrackingPacket(
        @Body packet: TrackingPacket,
    ): Response<Unit>

    @POST("tracking/location/bulk")
    suspend fun pushTrackingPacketBulk(
        @Body packets: List<TrackingPacket>,
    ): Response<Unit>
}
