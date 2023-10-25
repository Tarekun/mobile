package com.example.mobile.monitors

interface IMonitor {
    enum class MonitorType {
        AUDIO,
        WIFI,
        LTE
    }

    //TODO: maybe also add pause/resume functionality   
    fun startMonitoring(onStart: () -> Unit)
    fun stopMonitoring()
    fun readValue(): Double
}