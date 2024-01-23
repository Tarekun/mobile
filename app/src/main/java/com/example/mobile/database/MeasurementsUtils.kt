package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream

@Entity(tableName = "measurement")
@Serializable
data class Measurement(
    @Transient
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val signalStrength: Double,
    val classification: Classification,
    val monitor: MonitorVariant,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
)

@Dao
interface MeasurementDao {
    @Insert
    fun insert(measurement: Measurement): Long

    @Query("SELECT * FROM measurement")
    fun getAllMeasurements(): List<Measurement>

    @Query("SELECT * FROM measurement WHERE monitor = :monitor")
    fun getAllMeasurementsPerMonitor(monitor: MonitorVariant): List<Measurement>

    @Query("SELECT COUNT(*) FROM measurement WHERE monitor = :monitor")
    fun countMeasurementsPerMonitor(monitor: MonitorVariant): Int
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

    @Query("SELECT * FROM external_measurement")
    fun getAllExternalMeasurements(): List<ExternalMeasurement>

    @Query("SELECT * FROM external_measurement WHERE monitor = :monitor")
    fun getAllExternalMeasurementsPerMonitor(monitor: MonitorVariant): List<ExternalMeasurement>

    @Query("SELECT COUNT(*) FROM external_measurement WHERE monitor = :monitor")
    fun countExternalMeasurementsPerMonitor(monitor: MonitorVariant): Int
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
    fun makeMeasurement(
        signalStrength: Double,
        classification: Classification,
        variant: MonitorVariant,
        latitude: Double,
        longitude: Double,
    ): Measurement {

        return Measurement(
            0,
            signalStrength,
            classification,
            variant,
            Clock.System.now(),
            latitude,
            longitude
        )
    }

    fun storeMeasurement(measurement: Measurement) {
        DbManager.storeMeasurement(measurement)
    }

//    fun storeAudioMeasurement(decibels: Double, classification: Classification) {
//        DbManager.storeMeasurement(decibels, classification, MonitorVariant.AUDIO)
//    }
//
//    fun storeWifiMeasurement(decibels: Double, classification: Classification) {
//        DbManager.storeMeasurement(decibels, classification, MonitorVariant.WIFI)
//    }
//
//    fun storeLteMeasurement(decibels: Double, classification: Classification) {
//        DbManager.storeMeasurement(decibels, classification, MonitorVariant.LTE)
//    }

    fun storeExternalDump(jsonString: String) {
        val measurements: List<ExternalMeasurement> = Json.decodeFromString(jsonString)
        DbManager.storeManyExternalMeasurement(measurements)
    }


    fun getJsonLocalCollection(variant: MonitorVariant): String {
        val measurements: List<Measurement> = DbManager.findAllMeasurementsPerVariant(variant)
        return Json.encodeToString(measurements)
    }

    fun getJsonFullLocalCollection(): String {
        val measurements: MutableList<Measurement> =
            DbManager.findAllMeasurementsPerVariant(MonitorVariant.AUDIO).toMutableList()

        measurements += DbManager.findAllMeasurementsPerVariant(MonitorVariant.WIFI)
        measurements += DbManager.findAllMeasurementsPerVariant(MonitorVariant.LTE)
        return Json.encodeToString(measurements)
    }

    fun storeJsonDumpUri(dumpStream: InputStream?) {
        dumpStream?.use {
            val content = it.bufferedReader().use { reader -> reader.readText() }
            storeExternalDump(content)
        }
    }

    fun countLocalMeasurements(variant: MonitorVariant): Int {
        return DbManager.countMeasurement(variant)
    }

    fun countExternalMeasurements(variant: MonitorVariant): Int {
        return DbManager.countExternalMeasurement(variant)
    }
}