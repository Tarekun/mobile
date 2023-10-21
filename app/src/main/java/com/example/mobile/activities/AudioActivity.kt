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
import com.example.mobile.monitors.AudioMonitor
import com.example.mobile.ui.theme.MobileTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioActivity: ComponentActivity() {
    private val audioMonitor by lazy {
        AudioMonitor()
    }
    private var audioMonitoringJob: Job? = null
    private var value: Double by mutableStateOf(0.0)

    private fun startMonitoring() {
        value = 0.0
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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

    private fun stopMonitoring() {
        audioMonitoringJob?.cancel()
        audioMonitor.stopMonitoring()
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