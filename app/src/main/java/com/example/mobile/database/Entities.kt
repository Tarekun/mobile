package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.mobile.monitors.IMonitor.MonitorVariant
import java.util.Date

enum class Classification(val intValue: Int) {
    MAX(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    MIN(0),
    INVALID(-1),
    NO_DATA(-2) // Assegnato un valore negativo per indicare "NO_DATA"
}


@Entity(tableName = "measurement")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val signalStrength: Double,
    val classification: Classification,
    //TODO: should this be a MonitorVariant ??
    val monitor: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val grid : String? = null,
    val timestamp: Date
)

@Dao
interface MeasurementDao {
    @Insert
    fun insert(measurement: Measurement): Long

    @Query("SELECT * FROM measurement")
    fun getAllMeasurements(): List<Measurement>
    @Query("SELECT signalStrength FROM measurement WHERE grid = :gridName AND monitor = :monitorType ORDER BY timestamp DESC LIMIT 5")
    fun lastSignalStrength(gridName: String?, monitorType: MonitorVariant): List<Double>

}