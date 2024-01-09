package com.example.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import com.example.mobile.composables.Content
import com.example.mobile.composables.MonitorInfobox
import com.example.mobile.database.DbManager
import com.example.mobile.database.MonitorSettings
import com.example.mobile.database.SettingsTable
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.Monitor
import com.example.mobile.monitors.MonitorState
import com.example.mobile.monitors.MonitorVariant
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

    LaunchedEffect(monitor.variant) {
        withContext(Dispatchers.IO) {
            monitorSettings = when(monitor.variant) {
                MonitorVariant.AUDIO -> SettingsUtils.getStoredSettings().audio
                MonitorVariant.WIFI -> SettingsUtils.getStoredSettings().wifi
                MonitorVariant.LTE -> SettingsUtils.getStoredSettings().lte
            }
            initializing = false
        }
    }

    fun startRoutine() {
        // aggiunto così l'ide non si lamenta della chiamata a `readValue` nella coroutine
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            return
//        }

        value = 0.0
        monitor.startMonitoring {
            monitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while(true) {
                    //TODO: increase number of measurements taken
                    value = monitor.readValue()
                    delay(monitorSettings!!.monitorPeriod)
                }
            }
        }
    }

    val permissionRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRoutine()
        }
        else {
            //TODO: explain that the permission is needed and maybe take to the settings
        }
    }

    fun startMonitoring() {
        //TODO: permission check should be
        //check location enabled to localize every measurement
        //check specific permission per monitor
        //start monitoring
        if (ActivityCompat.checkSelfPermission(
                context,
                permissionForMonitor(monitor.variant)
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(permissionForMonitor(monitor.variant))
        }
        else {
            startRoutine()
        }

    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitor.stopMonitoring()
        value = 0.0
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
                value = value
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
                    MonitorState.CREATED -> startMonitoring()
                    MonitorState.STARTED -> stopMonitoring()
                    MonitorState.STOPPED -> {
                        monitor.reset()
                        startMonitoring()
                    }
                }
            }) {
                Text(text = "${if (monitor.currentStatus != MonitorState.STARTED) "Start" else "Stop"} monitoring")
            }
        }
//        Content(
//            currentVolume = value,
//            start = {
//                startMonitoring()
//            },
//            stop = {
//                stopMonitoring()
//            },
//            onPeriodUpdate = { updateMonitoringPeriod(it) }
//        )
    }
}