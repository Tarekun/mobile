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

    private val recorder by lazy {
        AudioMonitor()
    }
    private var value: Double by mutableStateOf(0.0)
    private var audioMonitoringJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRecordAudioPermission()

        setContent {
            MobileTheme {
                Content(
                    currentVolume = value,
                    start = {
                        //TODO: handle this in a proper way
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestRecordAudioPermission()
                        }
                        recorder.startMonitoring()
                        this.startAudioMonitoring()
                    },
                    stop = {
                        recorder.stopMonitoring()
                        this.stopAudioMonitoring()
                    })
            }
        }
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )
    }

    private fun startAudioMonitoring() {
        audioMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                value = recorder.readValue()
                delay(AudioMonitor.defaultTimePeriodMs)
            }
        }
    }

    private fun stopAudioMonitoring() {
        audioMonitoringJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        //rilascia le coroutine
        stopAudioMonitoring()
    }
}