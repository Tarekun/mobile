package com.example.mobile.monitors

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyCallback.SignalStrengthsListener
import android.telephony.TelephonyManager
import com.example.mobile.database.Classification

class LteMonitor(
    context: Context
): Monitor(context) {
    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var signalDbm: Double = Double.NEGATIVE_INFINITY
    private var signalStrengthListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    companion object {
        const val noSignalDbm = Double.NEGATIVE_INFINITY
    }


    private fun computeDbm(signalStrength: SignalStrength?): Double {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cellInfos = signalStrength?.cellSignalStrengths ?: emptyList()
            return if (cellInfos.isNotEmpty())
                cellInfos.sumOf{ it.dbm } / cellInfos.size.toDouble()
            else noSignalDbm
        }
        // legacy support
        else {
            return signalStrength?.cdmaDbm?.toDouble() ?: noSignalDbm
        }
    }

    override fun doStartMonitoring(): Boolean {
        // da Build.VERSION_CODES.S TelephonyManager#listen diventa deprecata e
        // si usa TelephonyManager#registerTelephonyCallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    signalDbm = computeDbm(signalStrength)
                }
            }

            // cast fatto per evitare messaggi di errore, in questo branch non verrà passato null
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                telephonyCallback as TelephonyCallback
            )
        } else {
            signalStrengthListener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    super.onSignalStrengthsChanged(signalStrength)
                    // Calculate signal strength value (example: LTE signal strength)
                    this@LteMonitor.signalDbm = computeDbm(signalStrength)
                }
            }
            telephonyManager.listen(
                signalStrengthListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )

        }

        return true
    }

    override fun doStopMonitoring(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // cast fatto per evitare messaggi di errore, in questo branch non verrà passato null
            telephonyManager.unregisterTelephonyCallback(telephonyCallback as TelephonyCallback)
        } else {
            signalStrengthListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
        signalStrengthListener = null
        telephonyCallback = null
        return true
    }

    override fun doReadValue(): Double {
        return signalDbm
    }

    override fun classifySignalStrength(dB: Double): Classification {
        return when(dB) {
            // in 50..65??
            in -65.0..0.0 -> Classification.MAX
            in -75.0..-65.0 -> Classification.HIGH
            in -85.0..-75.0 -> Classification.MEDIUM
            in -95.0..-85.0 -> Classification.LOW
            in Double.NEGATIVE_INFINITY..-95.0 -> Classification.MIN
            else -> Classification.INVALID
        }
    }

}