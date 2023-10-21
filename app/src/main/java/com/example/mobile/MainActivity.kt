package com.example.mobile

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.mobile.activities.AudioActivity
import com.example.mobile.activities.WifiActivity
import com.example.mobile.composables.ButtonVariant
import com.example.mobile.composables.ParametrizedButton
import com.example.mobile.monitors.IMonitor.MonitorType
import com.example.mobile.ui.theme.MobileTheme

class MainActivity : ComponentActivity() {

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            0
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var inUseMonitor: MonitorType by mutableStateOf(MonitorType.AUDIO)
        val monitors = listOf(MonitorType.AUDIO, MonitorType.WIFI, MonitorType.LTE)

        requestPermissions()
        val switchAudio = {
            startActivity(Intent(this, AudioActivity::class.java))
        }
        val switchWifi = {
            startActivity(Intent(this, WifiActivity::class.java))
        }

        setContent {
            MobileTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = "")
                            },
                            actions = {
                                monitors.forEach { monitor ->
                                    ParametrizedButton(
                                        //TODO: add icons for buttons
                                        onClick = {
                                            inUseMonitor = monitor
                                        },
                                        text = monitor.toString(),
                                        variant = if (inUseMonitor == monitor) ButtonVariant.FILLED else ButtonVariant.TONAL,
                                        modifier = Modifier,
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {

                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Button(onClick = switchAudio) {
                            Text(text = "Switch to audio")
                        }
                        Button(onClick = switchWifi) {
                            Text(text = "Switch to wifi")
                        }
                        Text(text = "content should be ${
                            if (inUseMonitor == MonitorType.AUDIO) "audio"
                            else if (inUseMonitor == MonitorType.WIFI) "wifi"
                            else "lte"
                        }")
                    }
                }
            }
        }
    }
}