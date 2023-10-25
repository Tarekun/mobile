package com.example.mobile.monitors

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.mobile.FileSaving.FileManager
import com.example.mobile.FileSaving.MonitorType
import database.DatabaseHelper

class WifiMonitor(
    activity: Activity,
    // importante che sia proprio l'applicationContext e non un Context derivato per release <=
    // Build.VERSION_CODES.N, tanto vale usare sempre questo di default se non ci causa problemi
    // reference: https://developer.android.com/reference/android/net/wifi/WifiManager
    applicationContext: Context
) {
    private val applicationContext = applicationContext
    private val fileManager = FileManager(applicationContext)
    private val activity = activity
    private val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val databaseHelper = DatabaseHelper(applicationContext)
    private val db = databaseHelper.writableDatabase

    companion object {
        // periodo di esecuzione delle misurazioni suggerito
        const val defaultTimePeriodMs: Long = 60000
    }

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun isLocationEnabled(): Boolean {
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

    fun startMonitoring(onStart: () -> Unit) {
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

    fun stopMonitoring() {

    }

    // value read in dBm
    fun readValue(): Double {
        //TODO: properly manage permissions
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

        //TODO: implement signal monitoring for both any network and only the connected one
        //TODO: get rid of logging
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
            val classification = fileManager.classifyValue(averageSignalStrength,MonitorType.WIFI)
            //fileManager.saveData(MonitorType.WIFI, "$averageSignalStrength ($classification)")
            val values = ContentValues().apply {
                put("valore", averageSignalStrength) // inserisci il valore desiderato per il campo 'valore'
                put("classificazione", classification) // inserisci il valore desiderato per il campo 'classificazione'
            }

            val newRowId = db.insert("wifi", null, values)

            db.close()

            return averageSignalStrength
        }
    }
}