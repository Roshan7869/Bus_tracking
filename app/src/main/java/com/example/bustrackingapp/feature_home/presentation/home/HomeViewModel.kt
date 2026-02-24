package com.example.bustrackingapp.feature_home.presentation.home

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrackingapp.core.domain.repository.LocationRepository
import com.example.bustrackingapp.core.util.LoggerUtil
import com.example.bustrackingapp.core.util.Resource
import com.example.bustrackingapp.feature_bus.domain.use_cases.GetNearbyBusesUseCase
import com.example.bustrackingapp.feature_bus_stop.domain.use_case.BusStopUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val busStopUseCases: BusStopUseCases,
    private val nearbyBusesUseCase: GetNearbyBusesUseCase,
    private val locationRepository: LocationRepository,
): ViewModel(){
    private val logger = LoggerUtil(c= "HomeViewModel")
    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        getLocationBusesStops()
    }

    fun getLocationBusesStops(){
        if (uiState.isLoadingLocation) return
        uiState = uiState.copy(isLoadingLocation = true)
        locationRepository.getCurrentLocation(
            callback = {
                uiState = uiState.copy(location = it, errorLocation = null, isLoadingLocation = false)
                // Parallel fetch — both calls launch concurrently in viewModelScope
                getNearbyBuses(isLoading = true)
                getNearbyStops(isLoading = true)
            },
            onError = {
                uiState = uiState.copy(errorLocation = it.message, isLoadingLocation = false)
            },
            isLive = false,
        )
    }

    fun getNearbyStops(isLoading: Boolean = false, isRefreshing: Boolean = false) {
        if (uiState.isLoadingLocation || uiState.isLoadingNearbyStops || uiState.isRefreshingNearbyStops) return
        if (uiState.location == null) {
            uiState = uiState.copy(errorNearbyStops = "Couldn't fetch current location")
            return
        }
        busStopUseCases.getNearbyBusStops(
            uiState.location!!.latitude,
            uiState.location!!.longitude
        ).onEach { result ->
            uiState = when (result) {
                is Resource.Success -> uiState.copy(
                    nearbyBusStops = result.data ?: emptyList(),
                    isLoadingNearbyStops = false,
                    isRefreshingNearbyStops = false,
                    errorNearbyStops = null,          // FIX: was errorNearbyBuses
                )
                is Resource.Error -> uiState.copy(
                    errorNearbyStops = result.message, // FIX: was errorNearbyBuses
                    isLoadingNearbyStops = false,
                    isRefreshingNearbyStops = false,
                )
                is Resource.Loading -> uiState.copy(
                    errorNearbyStops = null,           // FIX: was errorNearbyBuses
                    isLoadingNearbyStops = isLoading,
                    isRefreshingNearbyStops = isRefreshing,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun getNearbyBuses(isLoading: Boolean = false, isRefreshing: Boolean = false) {
        if (uiState.isLoadingLocation || uiState.isLoadingNearbyBuses || uiState.isRefreshingNearbyBuses) return
        if (uiState.location == null) {
            uiState = uiState.copy(errorNearbyBuses = "Couldn't fetch current location")
            return
        }
        nearbyBusesUseCase(
            uiState.location!!.latitude,
            uiState.location!!.longitude
        ).onEach { result ->
            uiState = when (result) {
                is Resource.Success -> uiState.copy(
                    nearbyBuses = result.data ?: emptyList(),
                    isLoadingNearbyBuses = false,
                    isRefreshingNearbyBuses = false,
                    errorNearbyBuses = null,
                )
                is Resource.Error -> uiState.copy(
                    errorNearbyBuses = result.message,
                    isLoadingNearbyBuses = false,
                    isRefreshingNearbyBuses = false,
                )
                is Resource.Loading -> uiState.copy(
                    errorNearbyBuses = null,
                    isLoadingNearbyBuses = isLoading,
                    isRefreshingNearbyBuses = isRefreshing,
                )
            }
        }.launchIn(viewModelScope)
    }
}
