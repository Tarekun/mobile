package com.example.mobile.monitors

import android.content.Context
import android.net.wifi.ScanResult
import android.util.Log
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import com.example.mobile.database.MeasurementsUtils
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

    protected abstract fun doStartMonitoring(): Boolean
    protected abstract fun doStopMonitoring(): Boolean
    protected abstract fun doReadValue(): Double

    override fun startMonitoring(onStart: () -> Unit) {
        checkStateOrFail(
            MonitorState.CREATED,
            {
                if (doStartMonitoring()) {
                    moveToStarted()
                    onStart()
                }
            }
        )
    }

    override fun stopMonitoring() {
        checkStateOrFail(
            MonitorState.STARTED,
            {
                if (doStopMonitoring()) {
                    moveToStopped()
                }
            }
        )
    }

    override fun readValue(): Double {
        return checkStateOrFail(
            MonitorState.STARTED,
            {
                val value = doReadValue()
                val classification = classifySignalStrength(value)
                when (variant) {
                    MonitorVariant.AUDIO ->
                        MeasurementsUtils.storeAudioMeasurement(value, classification)
                    MonitorVariant.WIFI ->
                        MeasurementsUtils.storeWifiMeasurement(value, classification)
                    MonitorVariant.LTE ->
                        MeasurementsUtils.storeLteMeasurement(value, classification)

                }
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