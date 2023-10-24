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
import com.example.mobile.monitors.AudioMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AudioMonitoringScreen(
    context: Activity
) {
    val audioMonitor by lazy {
        AudioMonitor()
    }
    var audioMonitoringJob: Job? = null
//    var value: Double by mutableStateOf(0.0)
    var value by remember { mutableStateOf(0.0) }

    fun startMonitoring() {
        //TODO: handle permissions properly
        //see https://medium.com/@rzmeneghelo/how-to-request-permissions-in-jetpack-compose-a-step-by-step-guide-7ce4b7782bd7
        value = 0.0
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                0
            )
            return
        }
        audioMonitor.startMonitoring {
            audioMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while(true) {
                    value = audioMonitor.readValue()
                    delay(AudioMonitor.defaultTimePeriodMs)
                }
            }
        }
    }

    fun stopMonitoring() {
        audioMonitoringJob?.cancel()
        audioMonitor.stopMonitoring()
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