package com.example.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
    var wifiMonitoringJob: Job? = null
    var value: Double by remember { mutableStateOf(0.0) }

    fun startMonitoring() {
        //TODO: handle permissions properly
        //see https://medium.com/@rzmeneghelo/how-to-request-permissions-in-jetpack-compose-a-step-by-step-guide-7ce4b7782bd7
        value = 0.0
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        wifiMonitor.startMonitoring {
            wifiMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while(true) {
                    value = wifiMonitor.readValue()
                    delay(WifiMonitor.defaultTimePeriodMs)
                }
            }
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