package com.example.bustrackingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bustrackingapp.core.presentation.navigation.Navigation
import com.example.bustrackingapp.core.util.LoggerUtil
import com.example.bustrackingapp.ui.theme.BusTrackingAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val logger = LoggerUtil(c = "MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BusTrackingAppTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val mainViewModel = viewModel<MainViewModel>()
                    val userType by mainViewModel.userType.collectAsState()
                    val startupRoute by mainViewModel.startupRoute.collectAsState()

                    logger.info("InitialScreen = $startupRoute, userType = $userType")

                    Navigation(
                        startDestination = startupRoute,
                        userType = userType,
                    )
                }
            }
        }
    }
}
