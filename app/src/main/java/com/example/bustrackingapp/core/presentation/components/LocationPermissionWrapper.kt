package com.example.bustrackingapp.core.presentation.components

import android.Manifest
import android.content.Context
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bustrackingapp.core.util.LoggerUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionWrapper(
    content: @Composable () -> Unit,
) {
    val logger = LoggerUtil(c = "LocationPermissionWrapper")
    val context = LocalContext.current

    var isLocationEnabled by rememberSaveable { mutableStateOf(true) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ),
    )

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        isLocationEnabled = activityResult.resultCode != 0
        showPermissionDialog = !isLocationEnabled
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (!locationPermissionsState.allPermissionsGranted) {
            showPermissionDialog = true
            return@LaunchedEffect
        }

        checkLocationSetting(
            context = context,
            onEnabled = {
                logger.info("Location Enabled")
                isLocationEnabled = true
                showPermissionDialog = false
            },
            onDisabled = {
                logger.info("Location Disabled")
                isLocationEnabled = false
                showPermissionDialog = true
                settingResultRequest.launch(it)
            },
        )
    }

    content()

    if ((!locationPermissionsState.allPermissionsGranted || !isLocationEnabled) && showPermissionDialog) {
        val allPermissionsRevoked =
            locationPermissionsState.permissions.size == locationPermissionsState.revokedPermissions.size

        val textToShow = if (locationPermissionsState.allPermissionsGranted) {
            "Please enable device location for nearby buses and real-time tracking."
        } else if (!allPermissionsRevoked) {
            "Approximate location granted. Enable precise location for better accuracy."
        } else if (locationPermissionsState.shouldShowRationale) {
            "Location permission is required for live bus updates and nearby stops."
        } else {
            "Enable location permission to continue with full features."
        }

        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location access") },
            text = {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = textToShow)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "You can continue in limited mode without this.")
                }
            },
            confirmButton = {
                CustomElevatedButton(
                    onClick = {
                        if (locationPermissionsState.allPermissionsGranted) {
                            checkLocationSetting(
                                context = context,
                                onEnabled = {
                                    isLocationEnabled = true
                                    showPermissionDialog = false
                                },
                                onDisabled = {
                                    isLocationEnabled = false
                                    showPermissionDialog = true
                                    settingResultRequest.launch(it)
                                },
                            )
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    text = if (locationPermissionsState.allPermissionsGranted) "Turn On Location" else "Grant Access",
                    borderRadius = 100.0,
                )
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Continue limited")
                }
            },
        )
    }
}

private fun checkLocationSetting(
    context: Context,
    onDisabled: (IntentSenderRequest) -> Unit,
    onEnabled: () -> Unit,
) {
    val logger = LoggerUtil()
    val locationRequest = LocationRequest.Builder(1000)
        .setMinUpdateIntervalMillis(1000)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .build()

    val client: SettingsClient = LocationServices.getSettingsClient(context)
    val builder: LocationSettingsRequest.Builder = LocationSettingsRequest
        .Builder()
        .addLocationRequest(locationRequest)

    val gpsSettingTask: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

    gpsSettingTask.addOnSuccessListener { onEnabled() }
    gpsSettingTask.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                val intentSenderRequest = IntentSenderRequest
                    .Builder(exception.resolution)
                    .build()
                onDisabled(intentSenderRequest)
            } catch (sendEx: IntentSender.SendIntentException) {
                logger.error(sendEx, "checkLocationSetting")
            }
        }
    }
}
