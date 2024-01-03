package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobile.composables.ButtonVariant
import com.example.mobile.composables.OptionSelect
import com.example.mobile.composables.ParametrizedButton
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