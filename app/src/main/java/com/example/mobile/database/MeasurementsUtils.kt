package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "measurement")
@Serializable
data class Measurement(
    @Transient
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
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

@Entity(tableName = "external_measurement")
@Serializable
data class ExternalMeasurement(
    @Transient
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val signalStrength: Double,
    val classification: Classification,
    val monitor: MonitorVariant,
    val timestamp: Instant
)

@Dao
interface ExternalMeasurementDao {
    @Insert
    fun insertMany(measurements: List<ExternalMeasurement>)

    @Query("SELECT * FROM measurement")
    fun getAllExternalMeasurements(): List<ExternalMeasurement>

    @Query("SELECT * FROM measurement WHERE monitor = :monitor")
    fun getAllExternalMeasurementsPerMonitor(monitor: String): List<ExternalMeasurement>
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

    fun storeExternalDump(jsonString: String) {
        val measurements: List<ExternalMeasurement> = Json.decodeFromString(jsonString)
        DbManager.storeManyExternalMeasurement(measurements)
    }


    fun getJsonLocalCollection(variant: MonitorVariant): String {
        val measurements: List<Measurement> = DbManager.findAllMeasurementsPerVariant(variant)
        return Json.encodeToString(measurements)
    }
}