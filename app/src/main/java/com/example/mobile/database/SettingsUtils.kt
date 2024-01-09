package com.example.mobile.database

import com.example.mobile.monitors.MonitorVariant

enum class SettingsNames {
    AUDIO_MONITOR_PERIOD,
    WIFI_MONITOR_PERIOD,
    LTE_MONITOR_PERIOD,
    AUDIO_MEASUREMENT_NUMBER,
    WIFI_MEASUREMENT_NUMBER,
    LTE_MEASUREMENT_NUMBER,
    GRID_UNIT_LENGTH
}

data class MonitorSettings(
    val monitorPeriod: Long,
    val measurementNumber: Int,
)

data class SettingsTable(
    val audio: MonitorSettings = MonitorSettings(1000 * 60, 10),
    val wifi: MonitorSettings = MonitorSettings(1000 * 60, 10),
    val lte: MonitorSettings = MonitorSettings(1000 * 60, 10),
    val gridUnitLength: Int = 10,
)

object SettingsUtils {
    //TODO: find improvement for this
    private fun makeSettingsTable(settings: List<Settings>): SettingsTable {
        val map = mutableMapOf<String, String>()
        for (setting in settings) {
            map[setting.name] = setting.value
        }
        return SettingsTable(
            MonitorSettings(
                (map[SettingsNames.AUDIO_MONITOR_PERIOD.name] ?: "").toLong(),
                (map[SettingsNames.AUDIO_MEASUREMENT_NUMBER.name] ?: "").toInt(),
            ),
            MonitorSettings(
                (map[SettingsNames.WIFI_MONITOR_PERIOD.name] ?: "").toLong(),
                (map[SettingsNames.WIFI_MEASUREMENT_NUMBER.name] ?: "").toInt(),
            ),
            MonitorSettings(
                (map[SettingsNames.LTE_MONITOR_PERIOD.name] ?: "").toLong(),
                (map[SettingsNames.LTE_MEASUREMENT_NUMBER.name] ?: "").toInt(),
            ),
            (map[SettingsNames.GRID_UNIT_LENGTH.name] ?: "").toInt(),
        )
    }

    private fun makeSettingsList(settings: SettingsTable): List<Settings> {
        return listOf(
            Settings(SettingsNames.AUDIO_MONITOR_PERIOD.name, settings.audio.monitorPeriod.toString()),
            Settings(SettingsNames.WIFI_MONITOR_PERIOD.name, settings.wifi.monitorPeriod.toString()),
            Settings(SettingsNames.LTE_MONITOR_PERIOD.name, settings.lte.monitorPeriod.toString()),
            Settings(SettingsNames.AUDIO_MEASUREMENT_NUMBER.name, settings.audio.measurementNumber.toString()),
            Settings(SettingsNames.WIFI_MEASUREMENT_NUMBER.name, settings.wifi.measurementNumber.toString()),
            Settings(SettingsNames.LTE_MEASUREMENT_NUMBER.name, settings.lte.measurementNumber.toString()),
            Settings(SettingsNames.GRID_UNIT_LENGTH.name, settings.gridUnitLength.toString()),
        )
    }

    fun getStoredSettings(): SettingsTable {
        val settings = checkFullSettingsList(DbManager.findAllSettings())
        return makeSettingsTable(settings)
    }

    private fun checkFullSettingsList(settings: List<Settings>): List<Settings> {
        val storedSettingsNames = settings.map { setting -> setting.name }

        for (settingName in SettingsNames.values()) {
            // settings are partial, a development issue => settings are restored to default
            if (settingName.name !in storedSettingsNames) {
                updateSettings(SettingsTable())
                return DbManager.findAllSettings()
            }
        }

        return settings
    }

    fun updateSettings(settings: SettingsTable) {
        DbManager.updateAllSettings(makeSettingsList(settings))
    }
}