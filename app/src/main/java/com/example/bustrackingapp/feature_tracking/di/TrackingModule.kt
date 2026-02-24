package com.example.bustrackingapp.feature_tracking.di

import com.example.bustrackingapp.feature_tracking.data.remote.api.TrackingApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackingModule {

    @Singleton
    @Provides
    fun provideTrackingApiService(retrofit: Retrofit): TrackingApiService {
        return retrofit.create(TrackingApiService::class.java)
    }
}
