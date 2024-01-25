package com.example.mobile.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.R
import com.example.mobile.composables.AlertSeverity
import com.example.mobile.composables.AlertTextbox
import com.example.mobile.composables.NumberSetting
import com.example.mobile.composables.OptionsSetting
import com.example.mobile.composables.SettingLayout
import com.example.mobile.composables.SwitchSetting
import com.example.mobile.database.MonitorSettings
import com.example.mobile.database.SettingsTable
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.MonitorVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun SettingsScreen(
    variant: MonitorVariant,
    navigateTo: (targetScreen: Screens) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    //TODO: properly initialize
    val gridSizes = listOf(10, 100, 1000)

    var settings: SettingsTable? by remember { mutableStateOf(null) }
    var monitorSettings: MonitorSettings? by remember { mutableStateOf(null) }
    var notifyInNewArea by remember { mutableStateOf(false) }
    var initializing by remember { mutableStateOf(true) }

    fun setMonitorSettings() {
        monitorSettings = when(variant) {
            MonitorVariant.AUDIO -> settings!!.audio
            MonitorVariant.WIFI -> settings!!.wifi
            MonitorVariant.LTE -> settings!!.lte
        }
    }

    fun startNotifyingInNewArea() {

    }
    fun stopNotifying() {

    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            settings = SettingsUtils.storedSettings
            setMonitorSettings()
            initializing = false
        }
    }
    LaunchedEffect(settings) {
        withContext(Dispatchers.IO) {
            if (!initializing) {
                SettingsUtils.updateSettings(settings!!)
                setMonitorSettings()
            }
        }
    }
    LaunchedEffect(variant) {
        if(!initializing) setMonitorSettings()
    }
    LaunchedEffect(notifyInNewArea) {
        if (notifyInNewArea) {
            startNotifyingInNewArea()
        }
        else {
            stopNotifying()
        }
    }

    if (initializing)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    else Column(
        modifier = Modifier.verticalScroll(scrollState)
    ) {
        AlertTextbox(
            severity = AlertSeverity.INFO,
            content = LocalContext.current.getString(R.string.settings_alert_note)
        )

        NumberSetting(
            title = context.getString(R.string.period_setting_title),
            description = context.getString(R.string.period_setting_desc),
            value = monitorSettings?.monitorPeriod ?: 0,
            onChange = {
                when(variant) {
                    MonitorVariant.AUDIO -> settings = settings!!.copy(
                        audio = MonitorSettings(
                            it,
                            settings!!.audio.measurementNumber,
                        )
                    )
                    MonitorVariant.WIFI -> settings = settings!!.copy(
                        wifi = MonitorSettings(
                            it,
                            settings!!.wifi.measurementNumber,
                        )
                    )
                    MonitorVariant.LTE -> settings = settings!!.copy(
                        lte = MonitorSettings(
                            it,
                            settings!!.lte.measurementNumber,
                        )
                    )
                }
            },
            min = 1
        )
        NumberSetting(
            title = context.getString(R.string.measurement_setting_title),
            description = context.getString(R.string.measurement_setting_desc),
            value = monitorSettings?.measurementNumber?.toLong() ?: 0,
            onChange = {
                when(variant) {
                    MonitorVariant.AUDIO -> settings = settings!!.copy(
                        audio = MonitorSettings(
                            settings!!.audio.monitorPeriod,
                            it.toInt()
                        )
                    )
                    MonitorVariant.WIFI -> settings = settings!!.copy(
                        wifi = MonitorSettings(
                            settings!!.wifi.monitorPeriod,
                            it.toInt()
                        )
                    )
                    MonitorVariant.LTE -> settings = settings!!.copy(
                        lte = MonitorSettings(
                            settings!!.lte.monitorPeriod,
                            it.toInt()
                        )
                    )
                }
            },
            min = 1
        )
        OptionsSetting(
            title = context.getString(R.string.grid_setting_title),
            description = context.getString(R.string.grid_setting_desc),
            value = settings?.gridUnitLength ?: 0,
            onChange = {
                settings = settings!!.copy(
                    gridUnitLength = it
                )
            },
            options = gridSizes
        )
        SwitchSetting(
            label = "label",
            onClick = {
                notifyInNewArea = !notifyInNewArea
            },
            value = notifyInNewArea,
        )

        SettingLayout(
            title = context.getString(R.string.settingscreen_export_button_description),
            inputField = {
                OutlinedButton(
                    onClick = { navigateTo(Screens.EXPORT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(context.getString(R.string.settingscreen_export_button))
                }
            }
        )
    }
}