package com.example.mobile.database

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.lang.IllegalArgumentException
import java.util.Date

const val DATABASE_NAME = "mydatabase.db"

private class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Instant {
        return value.let { Instant.fromEpochMilliseconds(value) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant): Long {
        return date.toEpochMilliseconds()
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

    @TypeConverter
    fun variantToString(variant: MonitorVariant): String {
        return variant.toString()
    }

    @TypeConverter
    fun stringToVariant(variant: String): MonitorVariant {
        for (monitorVariant in enumValues<MonitorVariant>()) {
            if (variant == monitorVariant.toString()) {
                return monitorVariant
            }
        }
        throw IllegalArgumentException(
            "The illegal monitor variant string \"${variant}\" was required. " +
            "Only possible values are ${MonitorVariant.values().map { "${it.name} " }}"
        )
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

    fun storeMeasurement(
        decibels: Double,
        classification: Classification,
        monitor: MonitorVariant
    ) {
        var measurement = Measurement(
            0,
            decibels,
            classification,
            monitor,
            Clock.System.now()
        )

         measurementDao.insert(measurement)
    }

    fun findAllMeasurementsPerVariant(variant: MonitorVariant): List<Measurement> {
        return measurementDao.getAllMeasurementsPerMonitor(variant.name)
    }

    fun getAllMeasurements(): List<Measurement> {
        return measurementDao.getAllMeasurements()
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