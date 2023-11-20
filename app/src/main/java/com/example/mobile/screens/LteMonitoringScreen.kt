package com.example.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.example.mobile.composables.Content
import com.example.mobile.monitors.AudioMonitor
import com.example.mobile.monitors.LteMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LteMonitoringScreen(
    context: Activity
) {
    val lteMonitor by lazy {
        LteMonitor(context)
    }
    var lteMonitoringJob: Job? by remember { mutableStateOf(null) }
    var value by remember { mutableStateOf(0.0) }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    fun startRoutine() {
        // aggiunto così l'ide non si lamenta della chiamata a `readValue` nella coroutine
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        value = 0.0
        lteMonitor.startMonitoring {
            lteMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while(true) {
                    value = lteMonitor.readValue()
                    delay(1000)
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
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        } else {
            startRoutine()
        }

    }

    fun stopMonitoring() {
        lteMonitoringJob?.cancel()
        lteMonitor.stopMonitoring()
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