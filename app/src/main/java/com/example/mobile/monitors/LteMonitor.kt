package com.example.mobile.monitors

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.TelephonyCallback.SignalStrengthsListener

class LteMonitor(
    context: Context
): IMonitor {
    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var signalDbm: Double = 0.0

    private var context = context
    private var signalStrengthListener: PhoneStateListener? = null
    private val telephonyCallback = object : TelephonyCallback(), SignalStrengthsListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            // Calculate signal strength value (example: LTE signal strength)
            signalDbm = computeDbm(signalStrength)
        }
    }

    //TODO: properly implement this function
    private fun computeDbm(signalStrength: SignalStrength?): Double {
        var baseValue = signalStrength?.level?.toDouble() ?: 0.0
        return baseValue * 4.0
    }

    override fun startMonitoring(onStart: () -> Unit) {
        // da Build.VERSION_CODES.S TelephonyManager#listen diventa deprecata e
        // si usa TelephonyManager#registerTelephonyCallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                telephonyCallback
            )
        } else {
            signalStrengthListener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    super.onSignalStrengthsChanged(signalStrength)
                    // Calculate signal strength value (example: LTE signal strength)
                    this@LteMonitor.signalDbm = computeDbm(signalStrength)
                }
            }
            telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        }
        onStart()
    }

    override fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback)
        } else {
            signalStrengthListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    override fun readValue(): Double {
        //TODO: implement db storing
        return signalDbm
    }
}