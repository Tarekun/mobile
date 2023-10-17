package com.example.mobile.monitors

interface IMonitor {
    //TODO: maybe also add pause/resume functionality   
    fun startMonitoring()
    fun stopMonitoring()
    fun readValue(): Double
}