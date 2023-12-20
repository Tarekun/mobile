package com.example.mobile.monitors

import android.content.Context
import com.example.mobile.database.Classification

interface IMonitor {
    enum class MonitorVariant {
        AUDIO,
        WIFI,
        LTE,
        MAP
    }

    //TODO: maybe also add pause/resume functionality   
    fun startMonitoring(onStart: () -> Unit)
    fun stopMonitoring()
    fun readValue(context: Context): Double
    fun classifySignalStrength(dB: Double): Classification
}