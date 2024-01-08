package com.example.mobile.monitors

import android.content.Context
import android.net.wifi.ScanResult
import android.util.Log
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import java.lang.IllegalStateException

enum class MonitorVariant {
    AUDIO,
    WIFI,
    LTE
}

enum class MonitorState {
    CREATED, STARTED, STOPPED
}

interface IMonitor {
    val variant: MonitorVariant
    fun startMonitoring(onStart: () -> Unit)
    fun stopMonitoring()
    fun reset()
    fun readValue(): Double
    fun classifySignalStrength(dB: Double): Classification
}

abstract class Monitor(
    protected val context: Context
): IMonitor {
    private var state = MonitorState.CREATED
    val currentStatus: MonitorState
        get() = state
    override val variant: MonitorVariant
        get() = when (this) {
            is AudioMonitor -> MonitorVariant.AUDIO
            is WifiMonitor -> MonitorVariant.WIFI
            is LteMonitor -> MonitorVariant.LTE
            else -> error("Unexpected implementation: $this")
        }

    private fun moveToStarted() {
        state = MonitorState.STARTED
    }

    private fun moveToStopped() {
        state = MonitorState.STOPPED
    }

    private fun moveToCreated() {
        state = MonitorState.CREATED
    }

    protected fun <T> checkStateOrFail(
        targetState: MonitorState,
        operation: () -> T,
        details: String = ""
    ): T {
        if (currentStatus != targetState) {
            throw IllegalStateException(
                "the monitor tried to do an operation which wasn't allowed in its current state (${state.name}). $details"
            )
        }
        else {
            return operation()
        }
    }

    protected abstract fun doStartMonitoring(onStart: () -> Unit): Boolean
    protected abstract fun doStopMonitoring(): Boolean
    protected abstract fun doReadValue(): Double

    override fun startMonitoring(onStart: () -> Unit) {
        val success = doStartMonitoring(onStart = onStart)
        if (success) moveToStarted()
    }

    override fun stopMonitoring() {
        val success = doStopMonitoring()
        if (success) moveToStopped()
    }

    override fun readValue(): Double {
        return checkStateOrFail(
            MonitorState.STARTED,
            {
                val value = doReadValue()
                val classification = classifySignalStrength(value)
                DbManager.storeWifiMeasurement(value, classification)
                value
            }
        )
    }

    override fun reset() {
        if (currentStatus != MonitorState.STOPPED) {
            stopMonitoring()
        }
        moveToCreated()
    }
}