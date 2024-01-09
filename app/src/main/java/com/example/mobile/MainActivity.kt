package com.example.mobile

import android.content.Intent
import android.net.Uri
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
import androidx.core.content.ContextCompat
import com.example.mobile.composables.OptionSelect
import com.example.mobile.database.DbManager
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.monitors.AudioMonitor
import com.example.mobile.monitors.LteMonitor
import com.example.mobile.monitors.MonitorVariant
import com.example.mobile.monitors.WifiMonitor
import com.example.mobile.screens.ExportScreen
import com.example.mobile.screens.MonitoringScreen
import com.example.mobile.screens.NavigationHistory
import com.example.mobile.screens.Screens
import com.example.mobile.screens.SettingsScreen
import com.example.mobile.ui.theme.MobileTheme
import java.io.File
import kotlin.io.path.writeText


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

        DbManager.init(applicationContext)

        var inUseMonitor: MonitorVariant by mutableStateOf(MonitorVariant.AUDIO)
        var currentScreen: Screens by mutableStateOf(Screens.MONITORING)
        val monitors = listOf(MonitorVariant.AUDIO, MonitorVariant.WIFI, MonitorVariant.LTE)

        val history = NavigationHistory(currentScreen)
        fun navigateTo(screen: Screens) {
            history.navigateTo(screen)
            currentScreen = history.currentScreen
        }
        fun navigateBack() {
            history.navigateBack()
            currentScreen = history.currentScreen
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
                                OptionSelect(
                                    label = "Select Monitor",
                                    options = monitors,
                                    value = inUseMonitor,
                                    onChange = { inUseMonitor = it },
                                    defaultOption = MonitorVariant.AUDIO
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    when (currentScreen) {
                                        // on the monitoring screen this is the settings button
                                        Screens.MONITORING -> navigateTo(Screens.SETTINGS)
                                        // otherwise it's the navigate back
                                        Screens.SETTINGS -> navigateBack()
                                        Screens.EXPORT -> navigateBack()
                                    }
                                }) {
                                    Icon(
                                        imageVector =
                                            if(currentScreen == Screens.MONITORING) Icons.Filled.Settings
                                            else Icons.Filled.ArrowBack,
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
                        when (currentScreen) {
                            Screens.MONITORING ->
                                MonitoringScreen(
                                    context = this@MainActivity,
                                    monitor = when (inUseMonitor) {
                                        MonitorVariant.AUDIO -> audioMonitor
                                        MonitorVariant.WIFI -> wifiMonitor
                                        MonitorVariant.LTE -> lteMonitor
                                    }
                                )
                            Screens.SETTINGS ->
                                SettingsScreen(
                                    variant = inUseMonitor,
                                    navigateTo = { navigateTo(it) }
                                )
                            Screens.EXPORT ->
                                ExportScreen(
                                    variant = inUseMonitor,
                                    startIntent = { this@MainActivity.startActivity(it) },
                                )
                        }
                    }
                }
            }
        }
    }
}