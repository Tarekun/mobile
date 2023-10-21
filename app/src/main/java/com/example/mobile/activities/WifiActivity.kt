package com.example.mobile.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.example.mobile.composables.Content
import com.example.mobile.monitors.WifiMonitor
import com.example.mobile.ui.theme.MobileTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WifiActivity: ComponentActivity() {
    private val wifiMonitor by lazy {
        WifiMonitor(this, applicationContext)
    }
    private var wifiMonitoringJob: Job? = null
    private var value: Double by mutableStateOf(0.0)

    private fun startMonitoring() {
        value = 0.0
        if (ActivityCompat.checkSelfPermission(
                this,
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

    private fun stopMonitoring() {
        wifiMonitoringJob?.cancel()
        wifiMonitor.stopMonitoring()
        value = 0.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MobileTheme {
                Content(
                    currentVolume = value,
                    start = {
                        startMonitoring()
                    },
                    stop = {
                        stopMonitoring()
                    })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }
}