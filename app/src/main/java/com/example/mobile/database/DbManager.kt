package com.example.mobile.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

private enum class MonitorVariant {
    AUDIO,
    WIFI,
    LTE
}

private class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(entities = [Measurement::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mydatabase.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DbManager(context: Context) {
    private var db: AppDatabase = AppDatabase.getDatabase(context)
    private var measurementDao: MeasurementDao = db.measurementDao()

    private fun storeMeasurement(decibels: Double, classification: Int, monitor: MonitorVariant) {
        var measurement = Measurement(
            0,
            decibels,
            classification,
            monitor.name,
            Date(System.currentTimeMillis())
        )

        runInCoroutine { measurementDao.insert(measurement) }
    }

    private fun runInCoroutine(operation: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            operation()
        }
    }

    fun storeAudioMeasurement(decibels: Double, classification: Int) {
        storeMeasurement(decibels, classification, MonitorVariant.AUDIO)
    }

    fun storeWifiMeasurement(decibels: Double, classification: Int) {
        storeMeasurement(decibels, classification, MonitorVariant.WIFI)
    }

    fun storeMobileMeasurement(decibels: Double, classification: Int) {
        storeMeasurement(decibels, classification, MonitorVariant.LTE)
    }

    fun getAllMeasurements(): List<Measurement> {
        return measurementDao.getAllMeasurements()
    }
}