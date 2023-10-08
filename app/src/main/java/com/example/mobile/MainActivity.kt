package com.example.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.mobile.ui.theme.MobileTheme

@Composable
fun Content(maxVolume: Int, currentVolume: Int, modifier: Modifier = Modifier) {
    MobileTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                Text(text = "Max Value: $maxVolume", modifier = modifier)
                Text(text = "Current Value: $currentVolume", modifier = modifier)
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var mediaRecorder: MediaRecorder
//    private var currentVolume by remember { mutableStateOf(0) }

    private val currentVolumeLiveData = MutableLiveData<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var maxVolume = 0

        // Initialize MediaRecorder
        mediaRecorder = MediaRecorder()

        // Check and request microphone permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        } else {
            // Initialize and start recording
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.setOutputFile("/dev/null")

            try {
                mediaRecorder.prepare()
                mediaRecorder.start()

                // Delay to allow MediaRecorder to stabilize
                Handler(Looper.getMainLooper()).postDelayed({
                    maxVolume = MediaRecorder.getAudioSourceMax()

                    // Periodically update currentVolume
                    val handler = Handler(Looper.getMainLooper())
                    handler.post(object : Runnable {
                        override fun run() {
                            currentVolumeLiveData.postValue(mediaRecorder.maxAmplitude)
                            handler.postDelayed(this, 1000) // Update every 1 second
                        }
                    })
                }, 1000) // Delay for 1 second
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Periodically update currentVolume
            val handler = Handler(Looper.getMainLooper())
            handler.post(object : Runnable {
                override fun run() {
                    currentVolumeLiveData.postValue(mediaRecorder.maxAmplitude)
//                    currentVolume = mediaRecorder.maxAmplitude
                    handler.postDelayed(this, 1000) // Update every 1 second
                }
            })
        }

        setContent {
            Content(maxVolume, currentVolumeLiveData.value ?: 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder.stop()
        mediaRecorder.release()
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 0
    }
}
