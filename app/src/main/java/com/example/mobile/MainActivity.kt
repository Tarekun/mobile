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
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.mobile.composables.OptionSelect
import com.example.mobile.database.DbManager
import com.example.mobile.monitors.AudioMonitor
import com.example.mobile.monitors.LteMonitor
import com.example.mobile.monitors.MonitorVariant
import com.example.mobile.monitors.WifiMonitor
import com.example.mobile.misc.LocationManager
import com.example.mobile.misc.NewAreaWorker
import com.example.mobile.misc.NotificationHelper
import com.example.mobile.map.MapScreen
import com.example.mobile.screens.NavigationHistory
import com.example.mobile.screens.Screens
import com.example.mobile.screens.MonitoringScreen
import com.example.mobile.screens.ProximityShareScreen
import com.example.mobile.screens.SettingsScreen
import com.example.mobile.ui.theme.MobileTheme
import java.util.concurrent.TimeUnit


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
    private lateinit var newAreaWorker: PeriodicWorkRequest
//    private lateinit var newAreaWorker: OneTimeWorkRequest

    private fun initializeSingletons() {
        DbManager.init(applicationContext)
        NotificationHelper.init(applicationContext)
        LocationManager.init(this)

        newAreaWorker = PeriodicWorkRequest.Builder(
            NewAreaWorker::class.java,
            30,
            TimeUnit.MINUTES
        ).build()
//        newAreaWorker = OneTimeWorkRequestBuilder<NewAreaWorker>()
//            .setInitialDelay(5, TimeUnit.SECONDS) // Set a short delay for testing
//            .build()
    }

    private fun startNotifyingInNewArea() {
        WorkManager.getInstance(this).enqueue(newAreaWorker)
    }
    private fun stopNotifying() {
        WorkManager.getInstance(this).cancelWorkById(newAreaWorker.id)
    }

    // TopAppBar è ancora in modalità experimental
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeSingletons()

        var inUseMonitor: MonitorVariant by mutableStateOf(MonitorVariant.AUDIO)
        var currentScreen: Screens by mutableStateOf(Screens.MONITORING)
        val monitors = listOf(MonitorVariant.AUDIO, MonitorVariant.WIFI, MonitorVariant.LTE)

        val history = NavigationHistory(currentScreen)
        fun navigateTo(screen: Screens) {
            history.navigateTo(screen)
            currentScreen = history.currentScreen
        }
        fun navigateBack() {
            if (!history.isLast()) {
                history.navigateBack()
                currentScreen = history.currentScreen
            }
        }

        // checks if the app was opened by a proximity share notification
        val endpointId = intent.getStringExtra(NotificationHelper.extraEndpointId) ?: ""
        if (endpointId != "") {
            navigateTo(Screens.PROXIMITY_SHARE)
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
                                    label = applicationContext.getString(R.string.select_monitor),
                                    options = monitors,
                                    value = inUseMonitor,
                                    onChange = { inUseMonitor = it },
                                    defaultOption = MonitorVariant.AUDIO
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    // on the monitoring screen this is the settings button
                                    if (currentScreen == Screens.MONITORING) {
                                        navigateTo(Screens.SETTINGS)
                                    }
                                    // otherwise it's the navigate back
                                    else {
                                        navigateBack()
                                    }
                                }) {
                                    Icon(
                                        imageVector =
                                            if(currentScreen == Screens.MONITORING) Icons.Filled.Settings
                                            else Icons.Filled.ArrowBack,
                                        contentDescription = applicationContext.getString(R.string.settings_button_description)
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
                                    },
                                    navigateTo = { navigateTo(it) }
                                )
                            Screens.SETTINGS ->
                                SettingsScreen(
                                    variant = inUseMonitor,
                                    navigateTo = { navigateTo(it) },
                                    startNotifyingInNewArea = {startNotifyingInNewArea()},
                                    stopNotifying = {stopNotifying()},
                                    startIntent = { this@MainActivity.startActivity(it) },
                                )
                            Screens.PROXIMITY_SHARE ->
                                ProximityShareScreen(
                                    endpointId = endpointId
                                )
                            Screens.MAP_SCREEN ->
                                MapScreen(variant = inUseMonitor)
                        }
                    }
                }
            }
        }
    }
}