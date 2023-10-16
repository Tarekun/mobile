package com.example.mobile

import android.Manifest
import android.os.Bundle
import android.util.Log
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

@Composable
fun Content(
    maxVolume: Int,
    currentVolume: Int,
    start: () -> Unit,
    stop: () -> Unit,
    read: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Max Value: $maxVolume", modifier = modifier)
            Text(text = "Current Value: $currentVolume", modifier = modifier)
            Button(onClick = start) {
                Text(text = "Start recorder")
            }
            Button(onClick = stop) {
                Text(text = "Stop recorder")
            }
            Button(onClick = read) {
                Text(text = "Read recorded value")
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private val recorder by lazy {
        AudioMonitor(applicationContext)
    }
    var value: Int by mutableStateOf(0)
    var audioMonitoringJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )

        setContent {
            MobileTheme {
                Content(
                    maxVolume = 0,
                    currentVolume = value,
                    start = {
                        recorder.startMonitoring()
                        this.startAudioMonitoring()
                    },
                    stop = {
                        recorder.stopMonitoring()
                        this.stopAudioMonitoring()
                    },
                    read = {
                        value = recorder.readValue()
                    })
            }
        }
    }

    fun startAudioMonitoring() {
        audioMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            //TODO: look in more detail coroutines and see if there's a better way to do this
            while(true) {
                value = recorder.readValue()
                Log.d("customtag", "passo di qua")
                delay(1000)
            }
        }
    }

    fun stopAudioMonitoring() {
        audioMonitoringJob?.cancel()
    }
}