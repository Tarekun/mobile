package com.example.mobile.monitors

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.annotation.RequiresPermission
import com.example.mobile.R
import com.example.mobile.database.Classification

class WifiMonitor(
    private val activity: Activity,
    // importante che sia proprio l'applicationContext e non un Context derivato per release <=
    // Build.VERSION_CODES.N, tanto vale usare sempre questo di default se non ci causa problemi
    // reference: https://developer.android.com/reference/android/net/wifi/WifiManager
    private val applicationContext: Context
): Monitor(applicationContext) {
    private val wifiManager =
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager =
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestSettingEnabled(title: String, message: String, intent: Intent) {
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(applicationContext.getString(R.string.ok)) { dialog, _ ->
            activity.startActivity(intent)
            dialog.dismiss()
        }
        builder.setNegativeButton(applicationContext.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    @RequiresPermission(value = "android.permission.ACCESS_FINE_LOCATION")
    override fun doStartMonitoring(): Boolean {
        if (!isWifiEnabled()) {
            val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            requestSettingEnabled(
                context.getString(R.string.enable_wifi_dialog_title),
                context.getString(R.string.enable_wifi_dialog_content),
                wifiIntent
            )
            return false
        }
        else if (!isLocationEnabled()) {
            val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            requestSettingEnabled(
                context.getString(R.string.enable_location_dialog_title),
                context.getString(R.string.enable_location_dialog_content),
                locationIntent
            )
            return false
        }
        else {
            return true
        }
    }

    override fun doStopMonitoring(): Boolean {
        // only monitor with no proper state
        return true
    }

    // value read in dBm
    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    override fun doReadValue(): Double {
        //TODO: implement signal monitoring for both any network and only the connected one
        //should work like this: use currently connected one, if not available scan everything
        val scanResults: List<ScanResult> = wifiManager.scanResults
        if (scanResults.isEmpty()) {
            return Double.NEGATIVE_INFINITY
        } else {
            // Calculate average signal strength in dBm
            val totalSignalStrength = scanResults.sumBy { it.level }

            return totalSignalStrength.toDouble() / scanResults.size
        }
    }

    override fun classifySignalStrength(dB: Double): Classification {
        return when(dB) {
            // in 30..50 ??
            in -45.0..0.0 -> Classification.MAX
            in -60.0..-45.0 -> Classification.HIGH
            in -70.0..-60.0 -> Classification.MEDIUM
            in -80.0..-70.0 -> Classification.LOW
            in Double.NEGATIVE_INFINITY..-80.0 -> Classification.MIN
            else -> Classification.INVALID
        }
    }

}