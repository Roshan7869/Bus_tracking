package com.example.bustrackingapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrackingapp.core.domain.repository.UserPrefsRepository
import com.example.bustrackingapp.core.presentation.navigation.ScreenRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPrefsRepository: UserPrefsRepository,
) : ViewModel() {

    private val tokenFlow = userPrefsRepository.getToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val userTypeFlow = userPrefsRepository.getUserType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val startupRoute: StateFlow<String> = combine(tokenFlow, userTypeFlow) { token, _ ->
        if (token.isBlank()) {
            ScreenRoutes.AuthScreen.route
        } else {
            ScreenRoutes.DashboardScreen.route
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenRoutes.SplashScreen.route)

    val userType: StateFlow<String> = userTypeFlow
}
