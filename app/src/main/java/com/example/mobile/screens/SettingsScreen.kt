package com.example.mobile.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.mobile.composables.AlertTextbox
import com.example.mobile.composables.CollapsableSettings
import com.example.mobile.composables.ExportSettings
import com.example.mobile.composables.NumberSetting
import com.example.mobile.composables.OptionsSetting
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
    startNotifyingInNewArea: () -> Unit,
    stopNotifying: () -> Unit,
    startIntent: (intent: Intent) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var settings: SettingsTable? by remember { mutableStateOf(null) }
    var monitorSettings: MonitorSettings? by remember { mutableStateOf(null) }
    var initializing by remember { mutableStateOf(true) }

    fun setMonitorSettings() {
        monitorSettings = when(variant) {
            MonitorVariant.AUDIO -> settings!!.audio
            MonitorVariant.WIFI -> settings!!.wifi
            MonitorVariant.LTE -> settings!!.lte
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            settings = SettingsUtils.storedSettings
            setMonitorSettings()
            initializing = false
        }
    }
    LaunchedEffect(variant) {
        if(!initializing) setMonitorSettings()
    }

    LaunchedEffect(settings) {
        if (settings == null || initializing) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            if (!initializing) {
                SettingsUtils.updateSettings(settings!!)
                setMonitorSettings()
            }
        }

        if (settings!!.notifyInNewArea) {
            startNotifyingInNewArea()
        } else {
            stopNotifying()
        }
    }

    if (initializing)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    else Column(
        modifier = Modifier.verticalScroll(scrollState)
    ) {
        AlertTextbox(
            content = LocalContext.current.getString(R.string.settings_alert_note)
        )

        NumberSetting(
            title = context.getString(R.string.period_setting_title),
            description = context.getString(R.string.period_setting_desc),
            value = (monitorSettings?.monitorPeriod ?: 0) / 1000,
            onChange = {
                val newPeriodMs = 1000 * it
                when(variant) {
                    MonitorVariant.AUDIO -> settings = settings!!.copy(
                        audio = MonitorSettings(
                            newPeriodMs,
                            settings!!.audio.measurementNumber,
                        )
                    )
                    MonitorVariant.WIFI -> settings = settings!!.copy(
                        wifi = MonitorSettings(
                            newPeriodMs,
                            settings!!.wifi.measurementNumber,
                        )
                    )
                    MonitorVariant.LTE -> settings = settings!!.copy(
                        lte = MonitorSettings(
                            newPeriodMs,
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
            value = settings!!.gridUnitLength,
            onChange = {
                settings = settings!!.copy(
                    gridUnitLength = it
                )
            },
            options = SettingsUtils.gridSizes,
            getLabel = { "$it m" }
        )
        SwitchSetting(
            title = context.getString(R.string.settingscreen_external_title),
            label = if(settings!!.includeExternal) {
                context.getString(R.string.settingscreen_external_enabled)
            } else {
                context.getString(R.string.settingscreen_external_disabled)
            },
            onClick = {
                settings = settings!!.copy(includeExternal = !settings!!.includeExternal)
            },
            value = settings!!.includeExternal,
            contentDescription = context.getString(R.string.settingscreen_external_description)
        )

        SwitchSetting(
            title = context.getString(R.string.settingscreen_newarea_notification_title),
            label = if(settings!!.notifyInNewArea) {
                        context.getString(R.string.settingscreen_newarea_notification_enabled)
                    } else {
                        context.getString(R.string.settingscreen_newarea_notification_disabled)
                    },
            onClick = {
                settings = settings!!.copy(notifyInNewArea = !(settings!!.notifyInNewArea))
            },
            value = settings!!.notifyInNewArea,
            contentDescription = context.getString(R.string.settingscreen_newarea_notification_description)
        )
        CollapsableSettings(
            label = context.getString(R.string.settingscreen_collapse_notification_title),
            content = {
                Column {
                    SwitchSetting(
                        title = context.getString(R.string.settingscreen_all_monitors_title),
                        description = context.getString(R.string.settingscreen_all_monitors_description),
                        label = if (settings!!.notifyOnlyAllMonitors) {
                            context.getString(R.string.settingscreen_all_monitors_enabled)
                        } else {
                            context.getString(R.string.settingscreen_all_monitors_disabled)
                        },
                        onClick = {
                            settings =
                                settings!!.copy(notifyOnlyAllMonitors = !(settings!!.notifyOnlyAllMonitors))
                        },
                        value = settings!!.notifyOnlyAllMonitors,
                        contentDescription = context.getString(R.string.settingscreen_all_monitors_description)
                    )

                    OptionsSetting(
                        title = context.getString(R.string.settingscreen_notification_area_title),
                        value = settings!!.notifyOnlyAboveLength,
                        onChange = {
                            settings = settings!!.copy(
                                notifyOnlyAboveLength = it
                            )
                        },
                        options = SettingsUtils.gridSizes,
                        getLabel = { "$it m" }
                    )

                    OptionsSetting<Long>(
                        title = context.getString(R.string.settingscreen_notification_frequency_title),
                        description = context.getString(R.string.settingscreen_notification_frequency_description),
                        value = settings!!.notificationPeriodMs,
                        onChange = {
                            settings = settings!!.copy(
                                notificationPeriodMs = it
                            )
                        },
                        // no cooldown, half a day, a day, a week
                        options = SettingsUtils.notificationFrequencies,
                        getLabel = {
                            when (it) {
                                SettingsUtils.notificationFrequencies[0] ->
                                    context.getString(R.string.settingscreen_notification_frequency_always)
                                SettingsUtils.notificationFrequencies[1] ->
                                    context.getString(R.string.settingscreen_notification_frequency_tad)
                                SettingsUtils.notificationFrequencies[2] ->
                                    context.getString(R.string.settingscreen_notification_frequency_oad)
                                SettingsUtils.notificationFrequencies[3] ->
                                    context.getString(R.string.settingscreen_notification_frequency_oaw)
                                else ->
                                    it.toString()
                            }
                        }
                    )
                }
            }
        )
        CollapsableSettings(
            label = context.getString(R.string.settingscreen_export_menu),
            content = {
                ExportSettings(variant = variant, startIntent = startIntent)
            }
        )
    }
}