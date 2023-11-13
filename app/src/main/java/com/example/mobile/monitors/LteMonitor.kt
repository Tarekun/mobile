package com.example.mobile.monitors

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager

class LteMonitor(
    context: Context
): IMonitor {
    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var signalStrengthListener: PhoneStateListener? = null
    private var signalDbm: Double = 0.0

    //TODO: properly implement this function
    private fun computeDbm(baseValue: Double): Double {
        return baseValue * 4.0
    }

    override fun startMonitoring(onStart: () -> Unit) {
        signalStrengthListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                // Calculate signal strength value (example: LTE signal strength)
                this@LteMonitor.signalDbm =
                    computeDbm(signalStrength?.level?.toDouble() ?: 0.0)
            }
        }
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        onStart.invoke()
    }

    override fun stopMonitoring() {
        signalStrengthListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
    }

    override fun readValue(): Double {
        return signalDbm
    }
}