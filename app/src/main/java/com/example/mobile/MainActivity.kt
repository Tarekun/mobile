package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mobile.composables.OptionSelect
import com.example.mobile.monitors.AudioMonitor
import com.example.mobile.monitors.LteMonitor
import com.example.mobile.monitors.MonitorVariant
import com.example.mobile.monitors.WifiMonitor
import com.example.mobile.screens.MonitoringScreen
import com.example.mobile.ui.theme.MobileTheme



class MainActivity : ComponentActivity() {
    private val audioMonitor by lazy {
        AudioMonitor(applicationContext)
    }
    private val wifiMonitor by lazy {
        WifiMonitor(this, applicationContext)
    }
    private val lteMonitor by lazy {
        LteMonitor(applicationContext)
    }

    // TopAppBar è ancora in modalità experimental
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var inUseMonitor: MonitorVariant by mutableStateOf(MonitorVariant.AUDIO)
        var showSettings by mutableStateOf(false)
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
                                OptionSelect(
                                    label = "Select Monitor",
                                    options = monitors,
                                    onChange = { inUseMonitor = it },
                                    defaultOption = MonitorVariant.AUDIO
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { showSettings = !showSettings }) {
                                    Icon(
                                        imageVector = if(showSettings) Icons.Filled.ArrowBack
                                            else Icons.Filled.Settings,
                                        contentDescription = "Go to settings"
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
                        modifier = Modifier
                            .padding(innerPadding)
                    ) {
                        if (showSettings) {

                        }
                        else when (inUseMonitor) {
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