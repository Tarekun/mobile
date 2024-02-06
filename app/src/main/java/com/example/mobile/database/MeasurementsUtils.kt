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
    val grid : String? = null,
)

@Dao
interface MeasurementDao {
    @Insert
    fun insert(measurement: Measurement): Long

    @Query("SELECT * FROM measurement")
    fun getAllMeasurements(): List<Measurement>

    @Query("SELECT * FROM measurement WHERE monitor = :monitor")
    fun getAllMeasurementsPerMonitor(monitor: MonitorVariant): List<Measurement>

    @Query("SELECT * FROM measurement WHERE monitor = :monitor LIMIT :maxNumber")
    fun getAllMeasurementsPerMonitor(monitor: MonitorVariant, maxNumber: Int): List<Measurement>

    @Query("SELECT COUNT(*) FROM measurement WHERE monitor = :monitor")
    fun countMeasurementsPerMonitor(monitor: MonitorVariant): Int

    @Query("SELECT EXISTS(" +
           "SELECT * FROM measurement " +
           "WHERE latitude BETWEEN :bottom AND :top AND longitude BETWEEN :left AND :right)")
    fun isNewArea(top: Double, bottom: Double, left: Double, right: Double): Boolean
    
    @Query("SELECT signalStrength FROM measurement WHERE grid = :gridName AND monitor = :monitorType ORDER BY timestamp DESC LIMIT 5")
    fun lastSignalStrength(gridName: String?, monitorType: MonitorVariant): List<Double>
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
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
)

@Dao
interface ExternalMeasurementDao {
    @Insert
    fun insertMany(measurements: List<ExternalMeasurement>)

    @Query("SELECT * FROM external_measurement")
    fun getAllExternalMeasurements(): List<ExternalMeasurement>

    @Query("SELECT * FROM external_measurement WHERE monitor = :monitor")
    fun getAllExternalMeasurementsPerMonitor(monitor: MonitorVariant): List<ExternalMeasurement>

    @Query("SELECT * FROM external_measurement WHERE monitor = :monitor LIMIT :maxNumber")
    fun getAllExternalMeasurementsPerMonitor(monitor: MonitorVariant, maxNumber: Int): List<ExternalMeasurement>

    @Query("SELECT COUNT(*) FROM external_measurement WHERE monitor = :monitor")
    fun countExternalMeasurementsPerMonitor(monitor: MonitorVariant): Int
}

enum class Classification(val intValue: Int) {
    MAX(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    MIN(0),
    INVALID(-1),
    NO_DATA(-2) // Assegnato un valore negativo per indicare "NO_DATA"
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
        DbManager.measurementDao.insert(measurement)
    }

    fun storeExternalDump(jsonString: String) {
        val measurements: List<ExternalMeasurement> = Json.decodeFromString(jsonString)
        DbManager.externalMeasurementDao.insertMany(measurements)
    }


    fun getJsonLocalCollection(variant: MonitorVariant): String {
        val measurements: List<Measurement> =
            DbManager.measurementDao.getAllMeasurementsPerMonitor(variant)
        return Json.encodeToString(measurements)
    }

    fun getJsonFullLocalCollection(): String {
        val measurements = DbManager.measurementDao.getAllMeasurements()
        return Json.encodeToString(measurements)
    }

    fun storeJsonDumpUri(dumpStream: InputStream?) {
        dumpStream?.use {
            val content = it.bufferedReader().use { reader -> reader.readText() }
            storeExternalDump(content)
        }
    }

    fun countLocalMeasurements(variant: MonitorVariant): Int {
        return DbManager.measurementDao.countMeasurementsPerMonitor(variant)
    }

    fun countExternalMeasurements(variant: MonitorVariant): Int {
        return DbManager.externalMeasurementDao.countExternalMeasurementsPerMonitor(variant)
    }

    fun getLocalMeasurements(variant: MonitorVariant): List<Measurement> {
        return DbManager.measurementDao.getAllMeasurementsPerMonitor(variant)
    }

    fun getLocalMeasurements(variant: MonitorVariant, maxNumber: Int): List<Measurement> {
        return DbManager.measurementDao.getAllMeasurementsPerMonitor(variant, maxNumber)
    }

    fun getAllExternalMeasurements(variant: MonitorVariant, maxNumber: Int): List<ExternalMeasurement> {
        return DbManager.externalMeasurementDao.getAllExternalMeasurementsPerMonitor(variant, maxNumber)
    }

    fun isNewArea(top: Double, bottom: Double, left: Double, right: Double): Boolean {
        return DbManager.measurementDao.isNewArea(top, bottom, left, right)
    }
}