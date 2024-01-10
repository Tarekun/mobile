package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "measurement")
@Serializable
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val signalStrength: Double,
    val classification: Classification,
    val monitor: MonitorVariant,
    val timestamp: Instant
)

@Dao
interface MeasurementDao {
    @Insert
    fun insert(measurement: Measurement): Long

    @Query("SELECT * FROM measurement")
    fun getAllMeasurements(): List<Measurement>

    @Query("SELECT * FROM measurement WHERE monitor = :monitor")
    fun getAllMeasurementsPerMonitor(monitor: String): List<Measurement>
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

    fun getJsonLocalCollection(variant: MonitorVariant): String {
        val measurements: List<Measurement> = DbManager.findAllMeasurementsPerVariant(variant)
        return Json.encodeToString(measurements)
    }
}