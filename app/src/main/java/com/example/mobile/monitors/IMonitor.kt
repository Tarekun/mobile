package com.example.mobile.monitors

interface IMonitor {
    fun startMonitoring()
    fun stopMonitoring()
    fun readValue(): Int
}