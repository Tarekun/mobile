package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.example.mobile.monitors.IMonitor
import java.util.Date

enum class Classification(val intValue: Int) {
    MAX(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    MIN(0),
    INVALID(-1)
}

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

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val name: String,
    val value: String
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE name = :name")
    fun findByName(name: String): Settings?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): List<Settings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSetting(setting: Settings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAllSettings(settings: List<Settings>)
}