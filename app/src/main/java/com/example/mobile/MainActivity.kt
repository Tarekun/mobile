package com.example.mobile

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.mobile.composables.ButtonVariant
import com.example.mobile.composables.ParametrizedButton
import com.example.mobile.monitors.IMonitor.MonitorType
import com.example.mobile.screens.AudioMonitoringScreen
import com.example.mobile.screens.LteMonitoringScreen
import com.example.mobile.screens.WifiMonitoringScreen
import com.example.mobile.ui.theme.MobileTheme

class MainActivity : ComponentActivity() {

    private fun requestAllPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            0
        )
    }

    // TopAppBar è ancora in modalità experimental
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var inUseMonitor: MonitorType by mutableStateOf(MonitorType.AUDIO)
        val monitors = listOf(MonitorType.AUDIO, MonitorType.WIFI, MonitorType.LTE)
        requestAllPermissions()
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
                ) {
                        innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        if (inUseMonitor == MonitorType.AUDIO)
                            AudioMonitoringScreen(context = this@MainActivity)
                        else if (inUseMonitor == MonitorType.WIFI)
                            WifiMonitoringScreen(context = this@MainActivity)
                        else
                            LteMonitoringScreen()
                    }
                }
            }
        }
    }
}