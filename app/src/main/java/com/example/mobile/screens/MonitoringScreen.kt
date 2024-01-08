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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
    var periodMs: Long by remember { mutableStateOf(1000) }
    var firstRender: Boolean by remember { mutableStateOf(true) }
    var showMap: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(periodMs) {
        if (firstRender) {
            withContext(Dispatchers.IO) {
                val storedPeriod: Long? = DbManager.findPeriodForMonitor(monitor.variant)
                if (storedPeriod != null) {
                    periodMs = storedPeriod
                }
            }
            firstRender = false
        }
        else {
            withContext(Dispatchers.IO) {
                DbManager.updatePeriodForMonitor(monitor.variant, periodMs)
            }
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
                    value = monitor.readValue()
                    delay(periodMs)
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

    fun updateMonitoringPeriod(newPeriod: Long) {
        require(newPeriod > 0) {
            "`updateMonitoringPeriod` argument `newPeriod` should be positive, was $newPeriod instead"
        }
        //TODO: store the updated period on the db
        periodMs = newPeriod
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonitorInfobox(
            variant = monitor.variant,
            monitorStatus = monitor.currentStatus,
            value = value
        )
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
        Content(
            currentVolume = value,
            start = {
                startMonitoring()
            },
            stop = {
                stopMonitoring()
            },
            onPeriodUpdate = { updateMonitoringPeriod(it) }
        )
    }
}