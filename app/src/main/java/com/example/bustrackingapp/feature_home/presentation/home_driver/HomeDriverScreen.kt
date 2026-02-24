package com.example.bustrackingapp.feature_home.presentation.home_driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

// Orange for offline, amber for syncing, green for fully live
private val ColorOffline   = Color(0xFFD84315)
private val ColorSyncing   = Color(0xFFF9A825)
private val ColorLive      = Color(0xFF2E7D32)
private val ColorOnBanner  = Color.White

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
        topBar = { TopAppBar(title = { Text("Driver Tracking Console") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Connectivity Banner ──────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.isTracking && uiState.isOffline,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                ConnectivityBanner(
                    backgroundColor = ColorOffline,
                    text = "No network — packets buffered locally (${uiState.bufferedCount})",
                )
            }

            AnimatedVisibility(
                visible = uiState.isTracking && uiState.isSyncing,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                ConnectivityBanner(
                    backgroundColor = ColorSyncing,
                    text = "Network restored — syncing ${uiState.bufferedCount} buffered packets...",
                )
            }

            // ── Input Fields ───────────────────────────────────────────────
            Text("Configure bus telemetry stream", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.busId,
                onValueChange = homeDriverViewModel::onBusIdChange,
                label = { Text("Bus ID") },
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.driverId,
                onValueChange = homeDriverViewModel::onDriverIdChange,
                label = { Text("Driver ID") },
                singleLine = true,
            )

            // ── Control Buttons ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { homeDriverViewModel.startTracking(context) },
                    enabled = !uiState.isTracking,
                ) { Text("Start Tracking") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { homeDriverViewModel.stopTracking(context) },
                    enabled = uiState.isTracking,
                ) { Text("Stop Tracking") }
            }

            // ── Live Status Card ───────────────────────────────────────────
            val statusColor = when {
                !uiState.isTracking -> MaterialTheme.colorScheme.surfaceVariant
                uiState.isOffline   -> ColorOffline.copy(alpha = 0.08f)
                else                -> ColorLive.copy(alpha = 0.08f)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusColor),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusRow(
                        label = "Status",
                        value = when {
                            !uiState.isTracking -> "INACTIVE"
                            uiState.isOffline   -> "OFFLINE — buffering"
                            uiState.isSyncing   -> "SYNCING backlog"
                            else                -> "LIVE"
                        },
                        valueColor = when {
                            !uiState.isTracking -> MaterialTheme.colorScheme.onSurfaceVariant
                            uiState.isOffline   -> ColorOffline
                            uiState.isSyncing   -> ColorSyncing
                            else                -> ColorLive
                        },
                    )
                    StatusRow("Last heartbeat", formatTs(uiState.lastHeartbeatTs))
                    StatusRow("Last upload",    formatTs(uiState.lastSuccessTs))
                    StatusRow("Buffered",       "${uiState.bufferedCount} packets")
                    StatusRow("Network",        uiState.networkType)
                    StatusRow(
                        "Battery",
                        if (uiState.batteryPercent >= 0) "${uiState.batteryPercent}%" else "--",
                        valueColor = when {
                            uiState.batteryPercent in 1..20 -> ColorOffline
                            else                            -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectivityBanner(backgroundColor: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = ColorOnBanner, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

private fun formatTs(ts: Long): String {
    if (ts <= 0L) return "--"
    val diffSec = ((System.currentTimeMillis() - ts) / 1000L).coerceAtLeast(0)
    return if (diffSec < 60) "${diffSec}s ago" else "${diffSec / 60}m ${diffSec % 60}s ago"
}
