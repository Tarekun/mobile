package com.example.mobile.monitors

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyCallback.SignalStrengthsListener
import android.telephony.TelephonyManager
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import com.example.mobile.monitors.MapMonitor.CurrentState.currentGridCell
import com.example.mobile.monitors.MapMonitor.CurrentState.currentLocation

class LteMonitor(
    context: Context
): IMonitor {
    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var signalDbm: Double = 0.0
    private val noSignalDbm = Double.NEGATIVE_INFINITY
    private val dbManager = DbManager(context)
    private val context = context
    private var signalStrengthListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null


    private fun computeDbm(signalStrength: SignalStrength?): Double {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cellInfos = signalStrength?.cellSignalStrengths ?: emptyList()
            //TODO: make sure this average ever makes sense
            return if (cellInfos.size > 0)
                cellInfos.sumOf{ it.dbm } / cellInfos.size.toDouble()
            else noSignalDbm
        }
        // legacy support
        else {
            return signalStrength?.cdmaDbm?.toDouble() ?: noSignalDbm
        }
    }

    override fun startMonitoring(onStart: () -> Unit) {
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
            telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        }
        onStart()
    }

    override fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // cast fatto per evitare messaggi di errore, in questo branch non verrà passato null
            telephonyManager.unregisterTelephonyCallback(telephonyCallback as TelephonyCallback)
        } else {
            signalStrengthListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    override fun readValue(context: Context): Double {
        val decibelValue = signalDbm
        val classification = classifySignalStrength(decibelValue)
        dbManager.storeAudioMeasurement(decibelValue, classification,currentLocation?.latitude, currentLocation?.longitude,currentGridCell)
        return signalDbm
    }

    override fun classifySignalStrength(dB: Double): Classification {
        return when(dB) {
            // in 50..65??
            in 0.0..-65.0 -> Classification.MAX
            in -65.0..-75.0 -> Classification.HIGH
            in -75.0..-85.0 -> Classification.MEDIUM
            in -85.0..-95.0 -> Classification.LOW
            in -95.0..Double.NEGATIVE_INFINITY -> Classification.MIN
            else -> Classification.INVALID
        }
    }
}