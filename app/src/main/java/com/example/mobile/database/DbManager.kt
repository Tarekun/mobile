package com.example.mobile.database

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.mobile.monitors.MonitorVariant
import java.util.Date

const val DATABASE_NAME = "mydatabase.db"

private class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return value.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun classificationToInt(classification: Classification): Int {
        return classification.intValue
    }

    @TypeConverter
    fun intToClassification(classificationValue: Int): Classification {
        return when(classificationValue) {
            0 -> Classification.MAX
            1 -> Classification.HIGH
            2 -> Classification.MEDIUM
            3 -> Classification.LOW
            4 -> Classification.MIN
            else -> Classification.INVALID
        }
    }
}

@Database(entities = [Measurement::class, Settings::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DbManager {
    private lateinit var db: AppDatabase
    private lateinit var measurementDao: MeasurementDao
    private lateinit var settingsDao: SettingsDao

    fun init(applicationContext: Context) {
        db = AppDatabase.getDatabase(applicationContext)
        measurementDao = db.measurementDao()
        settingsDao = db.settingsDao()
    }

    private fun storeMeasurement(
        decibels: Double,
        classification: Classification,
        monitor: MonitorVariant
    ) {
        var measurement = Measurement(
            0,
            decibels,
            classification,
            monitor.name,
            Date(System.currentTimeMillis())
        )

         measurementDao.insert(measurement)
    }

    fun storeAudioMeasurement(decibels: Double, classification: Classification) {
        storeMeasurement(decibels, classification, MonitorVariant.AUDIO)
    }

    fun storeWifiMeasurement(decibels: Double, classification: Classification) {
        storeMeasurement(decibels, classification, MonitorVariant.WIFI)
    }

    fun storeMobileMeasurement(decibels: Double, classification: Classification) {
        storeMeasurement(decibels, classification, MonitorVariant.LTE)
    }

    fun getAllMeasurements(): List<Measurement> {
        return measurementDao.getAllMeasurements()
    }

    fun findPeriodForMonitor(variant: MonitorVariant): Long? {
        val intervalSetting = settingsDao.findByName(
            when (variant) {
                MonitorVariant.AUDIO -> SettingsNames.AUDIO_MONITOR_PERIOD.name
                MonitorVariant.WIFI -> SettingsNames.WIFI_MONITOR_PERIOD.name
                MonitorVariant.LTE -> SettingsNames.LTE_MONITOR_PERIOD.name
            }
        )

        return intervalSetting?.value?.toLong()
    }

    fun updatePeriodForMonitor(variant: MonitorVariant, period: Long) {
        val setting = Settings(
            //name selection
            when (variant) {
                MonitorVariant.AUDIO -> SettingsNames.AUDIO_MONITOR_PERIOD.name
                MonitorVariant.WIFI -> SettingsNames.WIFI_MONITOR_PERIOD.name
                MonitorVariant.LTE -> SettingsNames.LTE_MONITOR_PERIOD.name
            },
            period.toString()
        )
        settingsDao.insertOrUpdateSetting(setting)
    }

    fun findSettingByName(name: String): Settings? {
        return settingsDao.findByName(name)
    }

    fun findAllSettings(): List<Settings> {
        return settingsDao.getAllSettings()
    }

    fun updateAllSettings(settings: List<Settings>) {
        settingsDao.insertOrUpdateAllSettings(settings)
    }
}