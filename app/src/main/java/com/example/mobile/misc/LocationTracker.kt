package com.example.mobile.misc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.properties.Delegates

class LocationTracker(private val activity: Activity) {
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000 * 60)
        .setGranularity(Granularity.GRANULARITY_FINE)
        .setWaitForAccurateLocation(true)
        .build()
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    private val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationCallback: LocationCallback
    // lateinit so these can't be read before calling startLocationRecording
    private var _latitude by Delegates.notNull<Double>()
    private var _longitude by Delegates.notNull<Double>()
    val latitude: Double
        get() = _latitude
    val longitude: Double
        get() = _longitude

    init {
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
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    @RequiresPermission(value = "android.permission.ACCESS_FINE_LOCATION")
    fun startLocationRecording() {
            val locationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationProvider = LocationManager.GPS_PROVIDER
            val lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)
            _latitude = lastKnownLocation?.latitude ?: 0.0
            _longitude = lastKnownLocation?.longitude ?: 0.0

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
    }

    fun stopLocationRecording() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun isWorking(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
    }

}