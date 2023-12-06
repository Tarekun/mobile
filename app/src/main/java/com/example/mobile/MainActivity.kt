package com.example.mobile

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
import com.example.mobile.composables.ButtonVariant
import com.example.mobile.composables.ParametrizedButton
import com.example.mobile.monitors.MonitorVariant
import com.example.mobile.screens.AudioMonitoringScreen
import com.example.mobile.screens.LteMonitoringScreen
import com.example.mobile.screens.WifiMonitoringScreen
import com.example.mobile.ui.theme.MobileTheme

class MainActivity : ComponentActivity() {
    val audioMonitor by lazy {
        AudioMonitor(applicationContext)
    }
    val wifiMonitor by lazy {
        WifiMonitor(this, applicationContext)
    }
    val lteMonitor by lazy {
        LteMonitor(applicationContext)
    }

    // TopAppBar è ancora in modalità experimental
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var inUseMonitor: MonitorVariant by mutableStateOf(MonitorVariant.AUDIO)
        val monitors = listOf(MonitorVariant.AUDIO, MonitorVariant.WIFI, MonitorVariant.LTE)

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
                        when (inUseMonitor) {
                            MonitorVariant.AUDIO -> MonitoringScreen(context = this@MainActivity, audioMonitor)
                            MonitorVariant.WIFI -> MonitoringScreen(context = this@MainActivity, wifiMonitor)
                            MonitorVariant.LTE -> MonitoringScreen(context = this@MainActivity, lteMonitor)
                        }
                    }
                }
            }
        }
    }
}