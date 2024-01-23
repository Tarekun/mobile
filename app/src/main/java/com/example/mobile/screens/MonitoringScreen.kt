package com.example.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.mobile.composables.MonitorInfobox
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.MonitorSettings
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.Monitor
import com.example.mobile.monitors.MonitorState
import com.example.mobile.monitors.MonitorVariant
import com.example.mobile.notification.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun permissionForMonitor(variant: MonitorVariant): String {
    return when (variant) {
        MonitorVariant.AUDIO -> Manifest.permission.RECORD_AUDIO
        MonitorVariant.WIFI -> Manifest.permission.ACCESS_FINE_LOCATION
        MonitorVariant.LTE -> Manifest.permission.READ_PHONE_STATE
    }
}

@Composable
fun MonitoringScreen(
    context: Activity,
    monitor: Monitor
) {
    var monitoringJob: Job? by remember { mutableStateOf(null) }
    var value: Double by remember { mutableStateOf(0.0) }
    var showMap: Boolean by remember { mutableStateOf(false) }
    var monitorSettings: MonitorSettings? by remember { mutableStateOf(null) }
    var initializing by remember { mutableStateOf(true) }
    var measurementsNumber: Int by remember { mutableStateOf(0) }
    var externalMeasurementsNumber: Int by remember { mutableStateOf(0) }

    LaunchedEffect(monitor.variant) {
        withContext(Dispatchers.IO) {
            monitorSettings = when(monitor.variant) {
                MonitorVariant.AUDIO -> SettingsUtils.getStoredSettings().audio
                MonitorVariant.WIFI -> SettingsUtils.getStoredSettings().wifi
                MonitorVariant.LTE -> SettingsUtils.getStoredSettings().lte
            }

            measurementsNumber = MeasurementsUtils.countLocalMeasurements(monitor.variant)
            externalMeasurementsNumber = MeasurementsUtils.countExternalMeasurements(monitor.variant)
            initializing = false
        }
    }

    @RequiresPermission(value = "android.permission.ACCESS_FINE_LOCATION")
    fun startMonitoring() {
        LocationManager.startLocationRecording()
        value = 0.0
        monitor.startMonitoring {
            monitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while(true) {
                    value = monitor.makeMeasurement().signalStrength
                    measurementsNumber++
                    delay(monitorSettings!!.monitorPeriod)
                }
            }
        }
    }

    val permissionRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitoring()
        }
        else {
            //TODO: explain that the permission is needed and maybe take to the settings
        }
    }

    fun checkPermissionsAndStartMonitoring() {
        // location is needed by every monitor to persist on db the coordinates of the measurement
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        // then we check for monitor specific permissions
        else if (ActivityCompat.checkSelfPermission(
                context,
                permissionForMonitor(monitor.variant)
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(permissionForMonitor(monitor.variant))
        }
        else {
            startMonitoring()
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitor.stopMonitoring()
        value = 0.0
        LocationManager.stopLocationRecording()
    }



    if (initializing)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    else Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        monitorSettings?.let {
            MonitorInfobox(
                variant = monitor.variant,
                monitorStatus = monitor.currentStatus,
                monitorSettings = it,
                currentValue = value,
                measurementsNumber = measurementsNumber,
                externalMeasurementsNumber = externalMeasurementsNumber,
            )
        }
        Row() {
            Button(
                onClick = { showMap = !showMap },
            ) {
                Text(text = "${if (showMap) "Hide" else "Show"} map")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                when (monitor.currentStatus) {
                    MonitorState.CREATED -> checkPermissionsAndStartMonitoring()
                    MonitorState.STARTED -> stopMonitoring()
                    MonitorState.STOPPED -> {
                        monitor.reset()
                        checkPermissionsAndStartMonitoring()
                    }
                }
            }) {
                Text(text = "${if (monitor.currentStatus != MonitorState.STARTED) "Start" else "Stop"} monitoring")
            }
        }
    }
}