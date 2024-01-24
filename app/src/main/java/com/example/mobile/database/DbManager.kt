package com.example.mobile.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.mobile.monitors.IMonitor.MonitorVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

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

@Database(entities = [Measurement::class], version = 3, exportSchema = false)
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DbManager(context: Context) {
    private var db: AppDatabase = AppDatabase.getDatabase(context)
    private var measurementDao: MeasurementDao = db.measurementDao()

    private fun storeMeasurement(
        decibels: Double,
        classification: Classification,
        monitor: MonitorVariant,
        latitude : Double? = null,
        longitude : Double? = null,
        grid : String? = null
    ) {
        var measurement = Measurement(
            0,
            decibels,
            classification,
            monitor.name,
            latitude,
            longitude,
            grid,
            Date(System.currentTimeMillis())
        )


        runInCoroutine { measurementDao.insert(measurement) }
    }

    private fun runInCoroutine(operation: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            operation()
        }
    }

    fun storeAudioMeasurement(decibels: Double, classification: Classification, latitude: Double? = null, longitude: Double? = null,grid : String? = null
    ) {
        storeMeasurement(decibels, classification, MonitorVariant.AUDIO, latitude, longitude, grid)
    }

    fun storeWifiMeasurement(decibels: Double, classification: Classification, latitude: Double, longitude: Double) {
        storeMeasurement(decibels, classification, MonitorVariant.WIFI, latitude, longitude)
    }

    fun storeMobileMeasurement(decibels: Double, classification: Classification, latitude: Double, longitude: Double) {
        storeMeasurement(decibels, classification, MonitorVariant.LTE, latitude, longitude)
    }

    fun getAllMeasurements(): List<Measurement> {
        return measurementDao.getAllMeasurements()
    }

    fun lastSignalStrength(gridName: String?, monitorType: MonitorVariant): List<Double>{
        return measurementDao.lastSignalStrength(gridName,monitorType)
    }
}