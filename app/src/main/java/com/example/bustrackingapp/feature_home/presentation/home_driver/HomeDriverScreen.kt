package com.example.bustrackingapp.feature_home.presentation.home_driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDriverScreen(
    homeDriverViewModel: HomeDriverViewModel = hiltViewModel(),
    snackbarState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState = homeDriverViewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarState.showSnackbar(it)
            homeDriverViewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Driver Tracking Console") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Configure bus telemetry stream")
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.busId,
                onValueChange = homeDriverViewModel::onBusIdChange,
                label = { Text("Bus ID") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.driverId,
                onValueChange = homeDriverViewModel::onDriverIdChange,
                label = { Text("Driver ID") },
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { homeDriverViewModel.startTracking(context) },
                enabled = !uiState.isTracking,
            ) {
                Text("Start live tracking")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { homeDriverViewModel.stopTracking(context) },
                enabled = uiState.isTracking,
            ) {
                Text("Stop live tracking")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (uiState.isTracking) "Tracking status: ACTIVE" else "Tracking status: INACTIVE")
                    Text("Last heartbeat: ${formatTs(uiState.lastHeartbeatTs)}")
                    Text("Last successful upload: ${formatTs(uiState.lastSuccessTs)}")
                    Text("Buffered packets: ${uiState.bufferedCount}")
                    Text("Network: ${uiState.networkType}")
                    Text("Battery: ${if (uiState.batteryPercent >= 0) "${uiState.batteryPercent}%" else "--"}")
                }
            }
        }
    }
}

private fun formatTs(ts: Long): String {
    if (ts <= 0L) return "--"
    val diffSec = ((System.currentTimeMillis() - ts) / 1000L).coerceAtLeast(0)
    return "${diffSec}s ago"
}
