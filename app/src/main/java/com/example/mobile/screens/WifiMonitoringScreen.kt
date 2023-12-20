package com.example.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.example.mobile.composables.Content
import com.example.mobile.monitors.WifiMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WifiMonitoringScreen(
    context: Activity
) {
    val wifiMonitor by lazy {
        //TODO: make sure this is correct
        WifiMonitor(context, context)
    }
    var wifiMonitoringJob: Job? by remember { mutableStateOf(null) }
    var value: Double by remember { mutableStateOf(0.0) }

    fun startRoutine() {
        // aggiunto così l'ide non si lamenta della chiamata a `readValue` nella coroutine
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        value = 0.0
        wifiMonitor.startMonitoring {
            wifiMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while(true) {
                    value = wifiMonitor.readValue(context)
                    delay(WifiMonitor.defaultTimePeriodMs)
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
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startRoutine()
        }
    }

    fun stopMonitoring() {
        wifiMonitoringJob?.cancel()
        wifiMonitor.stopMonitoring()
        value = 0.0
    }

    Content(
        currentVolume = value,
        start = {
            startMonitoring()
        },
        stop = {
            stopMonitoring()
        })
}