package com.example.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.mobile.monitors.AudioMonitor
import com.example.mobile.monitors.WifiMonitor
import com.example.mobile.ui.theme.MobileTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun Content(
    currentVolume: Double,
    start: () -> Unit,
    stop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val twoDecimalValue = (currentVolume * 100.0).roundToInt() / 100.0
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Current Value: $twoDecimalValue dBFS", modifier = modifier)
            Button(onClick = start) {
                Text(text = "Start recorder")
            }
            Button(onClick = stop) {
                Text(text = "Stop recorder")
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private val audioMonitor by lazy {
        AudioMonitor()
    }
    private val wifiMonitor by lazy {
        WifiMonitor(applicationContext)
    }
    private var value: Double by mutableStateOf(0.0)
    private var audioMonitoringJob: Job? = null
    private var wifiMonitoringJob: Job? = null

    private fun startMonitoring() {
        wifiMonitor.startMonitoring()
        this.startWifiMonitoring()
//        //TODO: handle this in a proper way
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestRecordAudioPermission()
//        }
//        audioMonitor.startMonitoring()
//        this.startAudioMonitoring()
    }

    private fun stopMonitoring() {
        wifiMonitor.stopMonitoring()
        this.stopWifiMonitoring()
//        audioMonitor.stopMonitoring()
//        this.stopAudioMonitoring()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

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

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
//                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION

            ),
            0
        )
    }


    //test monitoring for AudioMonitor
    private fun startAudioMonitoring() {
        value = 0.0
        audioMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                value = audioMonitor.readValue()
                delay(AudioMonitor.defaultTimePeriodMs)
            }
        }
    }
    private fun stopAudioMonitoring() {
        audioMonitoringJob?.cancel()
    }


    //test monitoring for WifiMonitor
    private fun startWifiMonitoring() {
        value = 0.0
        wifiMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                value = wifiMonitor.readValue()
                delay(WifiMonitor.defaultTimePeriodMs)
            }
        }
    }
    private fun stopWifiMonitoring() {
        wifiMonitoringJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        //rilascia le coroutine
        stopAudioMonitoring()
        stopWifiMonitoring()
    }
}