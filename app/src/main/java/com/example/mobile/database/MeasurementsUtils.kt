package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.mobile.monitors.MonitorVariant
import java.util.Date

@Entity(tableName = "measurement")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val signalStrength: Double,
    val classification: Classification,
    //TODO: should this be a MonitorVariant ??
    val monitor: String,
    val timestamp: Date
)

@Dao
interface MeasurementDao {
    @Insert
    fun insert(measurement: Measurement): Long

    @Query("SELECT * FROM measurement")
    fun getAllMeasurements(): List<Measurement>
}

enum class Classification(val intValue: Int) {
    MAX(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    MIN(0),
    INVALID(-1)
}

object MeasurementsUtils {
    fun storeAudioMeasurement(decibels: Double, classification: Classification) {
        DbManager.storeMeasurement(decibels, classification, MonitorVariant.AUDIO)
    }

    fun storeWifiMeasurement(decibels: Double, classification: Classification) {
        DbManager.storeMeasurement(decibels, classification, MonitorVariant.WIFI)
    }

    fun storeLteMeasurement(decibels: Double, classification: Classification) {
        DbManager.storeMeasurement(decibels, classification, MonitorVariant.LTE)
    }

    fun exportLocalCollection() {

    }
}