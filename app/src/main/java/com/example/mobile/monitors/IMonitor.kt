package com.example.mobile.monitors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import com.example.mobile.database.Settings

interface IMonitor {
    enum class MonitorVariant {
        AUDIO,
        WIFI,
        LTE
    }

    val variant: MonitorVariant

    //TODO: maybe also add pause/resume functionality   
    fun startMonitoring(onStart: () -> Unit)
    fun stopMonitoring()
    fun readValue(): Double
    fun classifySignalStrength(dB: Double): Classification
}