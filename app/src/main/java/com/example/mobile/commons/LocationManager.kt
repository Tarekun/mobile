package com.example.mobile.commons

import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.work.PeriodicWorkRequest
import com.example.mobile.MainActivity
import com.example.mobile.database.SettingsUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object LocationManager {
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000 * 60)
        .setGranularity(Granularity.GRANULARITY_FINE)
        .setWaitForAccurateLocation(true)
        .build()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var newAreaWorker: PeriodicWorkRequest

    //TODO: find sync system so that these values can't be read before they are properly initializeds
    private var _latitude: Double = 0.0
    private var _longitude: Double = 0.0
    val latitude: Double
        get() = _latitude
    val longitude: Double
        get() = _longitude

    fun init(activity: MainActivity) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                if (locationResult.locations.isNotEmpty()) {
                    var bestLocation = locationResult.locations[0]
                    for (location in locationResult.locations) {
                        if (location.accuracy > bestLocation.accuracy) {
                            bestLocation = location
                        }
                    }

                    _latitude = bestLocation.latitude
                    _longitude = bestLocation.longitude
                }
            }
        }

//        newAreaWorker = PeriodicWorkRequest.Builder(
//            NewAreaWorker::class.java,
//            15,
//            TimeUnit.MINUTES
//        ).build()
    }

    @RequiresPermission(value = "android.permission.ACCESS_FINE_LOCATION")
    fun startLocationRecording() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationRecording() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun startNotifyingInNewArea() {
//        withContext(Dispatchers.IO) {
//            val period = SettingsUtils.storedSettings.
//        }
    }

    fun reloadNotificationPeriod() {

    }

    fun stopNotifying() {

    }
}