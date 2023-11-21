package com.example.mobile.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import java.util.Date

@Entity(tableName = "measurement")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val signalStrength: Double,
    val classification: Int,
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