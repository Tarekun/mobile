package com.example.mobile.monitors

import android.util.Log
import com.example.mobile.database.Classification
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
    fun startMonitoring(onStart: () -> Unit)
    fun stopMonitoring()
    fun readValue(): Double
    fun classifySignalStrength(dB: Double): Classification
}

abstract class Monitor(): IMonitor {

    private var state = MonitorState.CREATED
    val currentStatus: MonitorState
        get() = state

    protected fun moveToStarted() {
        Log.d("miotag", "chiamata moveToStarted")
        state = MonitorState.STARTED
        Log.d("miotag", "chiamata moveToStarted ${state.name}")
    }

    protected fun moveToStopped() {
        Log.d("miotag", "chiamata moveToStopped")
        state = MonitorState.STOPPED
        Log.d("miotag", "chiamata moveToStopped ${state.name}")
    }

    protected fun <T> checkStateOrFail(
        targetState: MonitorState,
        operation: () -> T,
        details: String = ""
    ): T {
        if (state != targetState) {
            throw IllegalStateException(
                "the monitor tried to do an operation which wasn't allowed in its current state (${state.name}). $details"
            )
        }
        else {
            return operation()
        }
    }

    protected abstract fun doStartMonitoring(onStart: () -> Unit)
    protected abstract fun doStopMonitoring()

    override fun startMonitoring(onStart: () -> Unit) {
        doStartMonitoring(onStart = onStart)
        moveToStarted()
    }

    override fun stopMonitoring() {
        doStopMonitoring()
        moveToStopped()
    }
}