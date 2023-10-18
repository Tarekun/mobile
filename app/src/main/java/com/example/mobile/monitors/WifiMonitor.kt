package com.example.mobile.monitors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat

class WifiMonitor(
    // importante che sia proprio l'applicationContext e non un Context derivato per release <=
    // Build.VERSION_CODES.N, tanto vale usare sempre questo di default se non ci causa problemi
    // reference: https://developer.android.com/reference/android/net/wifi/WifiManager
    applicationContext: Context
): IMonitor {
    private val applicationContext: Context = applicationContext
    private val wifiManager: WifiManager = applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    companion object {
        // periodo di esecuzione delle misurazioni suggerito
        const val defaultTimePeriodMs: Long = 1000
    }

    override fun startMonitoring() {

    }

    override fun stopMonitoring() {

    }

    override fun readValue(): Double {
        if (ActivityCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return 0.0
        }

        val scanResults: List<ScanResult> = wifiManager.scanResults
        if (scanResults.isEmpty()) {
            Log.d("miotag", "SCAN RESULT VUOTO")
            return 0.0
        }
        else {
            Log.d("miotag", "SCAN CON ${scanResults.size} RISULTATI")
            // Calculate average signal strength in dBm
            val totalSignalStrength = scanResults.sumBy { it.level }
            val averageSignalStrength = totalSignalStrength.toDouble() / scanResults.size
            return averageSignalStrength
        }
    }
}