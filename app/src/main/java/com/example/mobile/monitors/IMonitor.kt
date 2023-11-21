package com.example.mobile.monitors

import com.example.mobile.database.Classification

interface IMonitor {
    enum class MonitorVariant {
        AUDIO,
        WIFI,
        LTE
    }

    //TODO: maybe also add pause/resume functionality   
    fun startMonitoring(onStart: () -> Unit)
    fun stopMonitoring()
    fun readValue(): Double
    fun classifySignalStrength(dB: Double): Classification
}