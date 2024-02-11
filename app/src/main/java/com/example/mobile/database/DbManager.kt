package com.example.mobile.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Instant
import java.lang.IllegalArgumentException

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
            4 -> Classification.MAX
            3 -> Classification.HIGH
            2 -> Classification.MEDIUM
            1 -> Classification.LOW
            0 -> Classification.MIN
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

@Database(entities = [Measurement::class, ExternalMeasurement::class, Settings::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun externalMeasurementDao(): ExternalMeasurementDao
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DbManager {
    private lateinit var db: AppDatabase
    private lateinit var _measurementDao: MeasurementDao
    private lateinit var _externalMeasurementDao: ExternalMeasurementDao
    private lateinit var _settingsDao: SettingsDao

    public val measurementDao
        get() = _measurementDao
    public val externalMeasurementDao
        get() = _externalMeasurementDao
    public val settingsDao
        get() = _settingsDao

    fun init(applicationContext: Context) {
        db = AppDatabase.getDatabase(applicationContext)
        _measurementDao = db.measurementDao()
        _externalMeasurementDao = db.externalMeasurementDao()
        _settingsDao = db.settingsDao()
    }
}