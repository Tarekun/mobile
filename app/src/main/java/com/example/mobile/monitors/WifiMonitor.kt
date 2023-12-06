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
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager

class WifiMonitor(
    private val activity: Activity,
    // importante che sia proprio l'applicationContext e non un Context derivato per release <=
    // Build.VERSION_CODES.N, tanto vale usare sempre questo di default se non ci causa problemi
    // reference: https://developer.android.com/reference/android/net/wifi/WifiManager
    private val applicationContext: Context
): Monitor() {
    private val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val dbManager = DbManager(applicationContext)
    override val variant: IMonitor.MonitorVariant
        get() = IMonitor.MonitorVariant.WIFI

    companion object {
        // periodo di esecuzione delle misurazioni suggerito
        const val defaultTimePeriodMs: Long = 60000
    }

    private fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestSettingEnabled(title: String, message: String, intent: Intent) {
        //TODO: check if a custom UI dialog would be better (example at the end of the function body)
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok") { dialog, _ ->
            activity.startActivity(intent)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

//        val dialog = Dialog(this)
//        dialog.setContentView(R.layout.custom_dialog_layout)
//        dialog.setContent(ComposableContent(?))
//        val enableButton = dialog.findViewById<Button>(R.id.enableButton)
//        enableButton.setOnClickListener {
//            val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
//            startActivity(wifiIntent)
//            dialog.dismiss()
//        }
//        dialog.show()
    }

    @RequiresPermission(value = "android.permission.ACCESS_FINE_LOCATION")
    override fun doStartMonitoring(onStart: () -> Unit) {
        checkStateOrFail(
            MonitorState.CREATED,
            {
                //TODO: add strings resources to avoid these hard coded strings
                if (!isWifiEnabled()) {
                    val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    requestSettingEnabled(
                        "Enable Wifi connection",
                        "In order to monitor the strength of wifi connections in your area you need to turn wifi connectivity",
                        wifiIntent
                    )
                } else if (!isLocationEnabled()) {
                    val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    requestSettingEnabled(
                        "Enable location",
                        "In order to monitor the strength of wifi connections android also requires to turn on the location.\n" +
                                "This is because if you monitor the available wifi connection you could get a good approximation of where " +
                                "the device is located. This does not mean that the application will check the device's location.",
                        locationIntent
                    )
                }
                if (isWifiEnabled() && isLocationEnabled()) {
                    onStart()
                }
            }
        )
    }

    override fun doStopMonitoring() {
        checkStateOrFail(MonitorState.STARTED, {

        })
    }

    // value read in dBm
    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    override fun readValue(): Double {
        return checkStateOrFail(
            MonitorState.STARTED,
            {
                //TODO: implement signal monitoring for both any network and only the connected one
                val scanResults: List<ScanResult> = wifiManager.scanResults
                if (scanResults.isEmpty()) { 0.0 }
                else {
                    // Calculate average signal strength in dBm
                    val totalSignalStrength = scanResults.sumBy { it.level }
                    val averageSignalStrength = totalSignalStrength.toDouble() / scanResults.size
                    val classification = classifySignalStrength(averageSignalStrength)

                    dbManager.storeWifiMeasurement(averageSignalStrength, classification)
                    averageSignalStrength
                }
            }
        )
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