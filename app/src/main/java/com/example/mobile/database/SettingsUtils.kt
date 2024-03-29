package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.datetime.Instant
import java.lang.IllegalArgumentException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val name: String,
    val value: String
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE name = :name")
    fun findByName(name: String): Settings?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): List<Settings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSetting(setting: Settings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAllSettings(settings: List<Settings>)
}

enum class SettingsNames {
    AUDIO_MONITOR_PERIOD,
    WIFI_MONITOR_PERIOD,
    LTE_MONITOR_PERIOD,
    AUDIO_MEASUREMENT_NUMBER,
    WIFI_MEASUREMENT_NUMBER,
    LTE_MEASUREMENT_NUMBER,
    GRID_UNIT_LENGTH,
    ENABLE_PROXIMITY_SHARE,
    NOTIFY_IN_NEW_AREA,
    NOTIFY_ONLY_ALL_MONITORS,
    NOTIFY_ONLY_ABOVE_LENGTH,
    NOTIFICATION_PERIOD,
    LAST_NOTIFICATION,
    INCLUDE_EXTERNAL
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
    val enableProximityShare: Boolean = false,
    val notifyInNewArea: Boolean = false,
    val notifyOnlyAllMonitors: Boolean = false,
    val notifyOnlyAboveLength: Int = 10,
    val notificationPeriodMs: Long = 24 * 60 * 60 * 1000,
    val lastNotification: Instant = Instant.fromEpochMilliseconds(0),
    val includeExternal: Boolean = true,
)

object SettingsUtils {
    public val gridSizes = listOf(10, 100, 1000)
    // instantaneous, twice a day, once a day, once a week
    public val notificationFrequencies = listOf<Long>(0, 1000*60*60*12, 1000*60*60*24, 1000*60*60*24*7)

    private fun getSettingNameOrThrow(
        property: KProperty1<SettingsTable, *>,
        forMonitorPeriod: Boolean = false
    ): String {
        val nameConversionTable = mapOf<String, String>(
            "audio" to "AUDIO",
            "wifi" to "WIFI",
            "lte" to "LTE",
            "gridUnitLength" to SettingsNames.GRID_UNIT_LENGTH.name,
            "enableProximityShare" to SettingsNames.ENABLE_PROXIMITY_SHARE.name,
            "notifyInNewArea" to SettingsNames.NOTIFY_IN_NEW_AREA.name,
            "notifyOnlyAllMonitors" to SettingsNames.NOTIFY_ONLY_ALL_MONITORS.name,
            "notifyOnlyAboveLength" to SettingsNames.NOTIFY_ONLY_ABOVE_LENGTH.name,
            "notificationPeriodMs" to SettingsNames.NOTIFICATION_PERIOD.name,
            "lastNotification" to SettingsNames.LAST_NOTIFICATION.name,
            "includeExternal" to SettingsNames.INCLUDE_EXTERNAL.name,
        )
        var result = nameConversionTable[property.name] ?: throw IllegalArgumentException("Property name ${property.name} is not mapped to any setting")

        if (property.name in listOf( "audio", "wifi", "lte")) {
            // removes the leading "AUDIO" substring from these monitor setting names since
            // the monitor specific name is already in `result`
            val audioWordLength = "AUDIO".length
            if (forMonitorPeriod) {
                result += SettingsNames.AUDIO_MONITOR_PERIOD.name.substring(audioWordLength)
            } else {
                result += SettingsNames.AUDIO_MEASUREMENT_NUMBER.name.substring(audioWordLength)
            }
        }
        return result
    }

    private fun makeSettingsTable(settings: List<Settings>): SettingsTable {
        val map = mutableMapOf<String, String>()
        for (setting in settings) {
            map[setting.name] = setting.value
        }
        return SettingsTable(
            audio = MonitorSettings(
                monitorPeriod = (map[SettingsNames.AUDIO_MONITOR_PERIOD.name] ?: "").toLong(),
                measurementNumber = (map[SettingsNames.AUDIO_MEASUREMENT_NUMBER.name] ?: "").toInt(),
            ),
            wifi = MonitorSettings(
                monitorPeriod = (map[SettingsNames.WIFI_MONITOR_PERIOD.name] ?: "").toLong(),
                measurementNumber = (map[SettingsNames.WIFI_MEASUREMENT_NUMBER.name] ?: "").toInt(),
            ),
            lte = MonitorSettings(
                monitorPeriod = (map[SettingsNames.LTE_MONITOR_PERIOD.name] ?: "").toLong(),
                measurementNumber = (map[SettingsNames.LTE_MEASUREMENT_NUMBER.name] ?: "").toInt(),
            ),
            gridUnitLength = (map[SettingsNames.GRID_UNIT_LENGTH.name] ?: "").toInt(),
            enableProximityShare = (map[SettingsNames.ENABLE_PROXIMITY_SHARE.name] ?: "").toBoolean(),
            notifyInNewArea = (map[SettingsNames.NOTIFY_IN_NEW_AREA.name] ?: "").toBoolean(),
            notifyOnlyAllMonitors = (map[SettingsNames.NOTIFY_ONLY_ALL_MONITORS.name] ?: "").toBoolean(),
            notifyOnlyAboveLength = (map[SettingsNames.NOTIFY_ONLY_ABOVE_LENGTH.name] ?: "").toInt(),
            notificationPeriodMs = (map[SettingsNames.NOTIFICATION_PERIOD.name] ?: "").toLong(),
            lastNotification = (Instant.parse(map[SettingsNames.LAST_NOTIFICATION.name] ?: "")),
            includeExternal = (map[SettingsNames.INCLUDE_EXTERNAL.name] ?: "").toBoolean(),
        )
    }

    private fun makeSettingsList(settings: SettingsTable): List<Settings> {
        val result = mutableListOf<Settings>()

        for (property in SettingsTable::class.memberProperties) {
            when (val value = property.get(settings)) {
                is MonitorSettings -> {
                    result.add(Settings(
                        getSettingNameOrThrow(property, forMonitorPeriod = true),
                        value.monitorPeriod.toString()
                    ))
                    result.add(Settings(
                        getSettingNameOrThrow(property, forMonitorPeriod = false),
                        value.measurementNumber.toString()
                    ))
                }
                is Int, is Long, is Boolean, is Instant -> {
                    result.add(Settings(getSettingNameOrThrow(property), value.toString()))
                }
            }
        }
        return result
    }

    // avoids name clash at compile time between this and the getter function
    @get:JvmName("storedSettings")
    public val storedSettings: SettingsTable
        get() = getStoredSettings()

    fun getStoredSettings(): SettingsTable {
        val settings = checkFullSettingsList(DbManager.settingsDao.getAllSettings())
        return makeSettingsTable(settings)
    }

    private fun checkFullSettingsList(settings: List<Settings>): List<Settings> {
        val storedSettingsNames = settings.map { setting -> setting.name }

        for (settingName in SettingsNames.values()) {
            // settings are partial, a development issue => settings are restored to default
            if (settingName.name !in storedSettingsNames) {
                updateSettings(SettingsTable())
                return DbManager.settingsDao.getAllSettings()
            }
        }

        return settings
    }

    fun updateSettings(settings: SettingsTable) {
        DbManager.settingsDao.insertOrUpdateAllSettings(makeSettingsList(settings))
    }

    fun updateSingleSetting(setting: SettingsNames, value: String) {
        DbManager.settingsDao.insertOrUpdateSetting(Settings(setting.name, value))
    }
}