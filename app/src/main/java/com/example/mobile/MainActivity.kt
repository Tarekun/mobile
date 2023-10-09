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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.mobile.recorders.AudioRecorder
import com.example.mobile.ui.theme.MobileTheme
import java.io.File

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
        AudioRecorder(applicationContext)
    }

    private var audioFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )
        var value = 0

        setContent {
            MobileTheme {
                Content(
                    maxVolume = 0,
                    currentVolume = value,
                    start = {
                        File(cacheDir, "audio.mp3").also {
                            recorder.start(it)
                            audioFile = it
                        }
                    },
                    stop = {
                        recorder.stop()
                    },
                    read = {
                        value = recorder.read()
                        Log.d("customtag", "read value to be displayed is $value")
                    })
            }
        }
    }
}