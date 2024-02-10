package com.example.mobile.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.mobile.MainActivity
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.MonitorSettings
import com.example.mobile.database.SettingsTable
import com.example.mobile.database.SettingsUtils
import com.example.mobile.map.MapGrid
import com.example.mobile.misc.LocationTracker
import com.example.mobile.monitors.MonitorVariant
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



@Composable
fun MapScreen(
    variant: MonitorVariant,
    mainActivity: MainActivity
) {
    val coroutineScope = rememberCoroutineScope()
    var initializing by remember { mutableStateOf(true) }
    var grid: MapGrid? by remember { mutableStateOf(null) }
    var settings: SettingsTable? by remember { mutableStateOf(null) }
    var monitorSettings: MonitorSettings? by remember { mutableStateOf(null) }
    var locationTracker: LocationTracker? by remember { mutableStateOf(null) }

    fun initMap(googleMap: GoogleMap) {
        val initialLatLng = LatLng(locationTracker!!.latitude, locationTracker!!.longitude)
        val marker = googleMap.addMarker(MarkerOptions().position(initialLatLng))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 20f))

        locationTracker!!.subscribeLocationUpdate { latitude, longitude ->
            marker?.position = LatLng(latitude, longitude)
        }

        initializing = false
    }

    fun renderMap(googleMap: GoogleMap) {
        val visibleRegion = googleMap.projection.visibleRegion.latLngBounds
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val measurements = MeasurementsUtils.getAllMeasurementsPerMonitorContainedIn(
                    variant = variant,
                    maxNumber = monitorSettings!!.measurementNumber,
                    top = visibleRegion.northeast.latitude,
                    bottom = visibleRegion.southwest.latitude,
                    left = visibleRegion.southwest.longitude,
                    right = visibleRegion.northeast.longitude
                ).toMutableList()
                if (settings!!.includeExternal) {
                    measurements += MeasurementsUtils.getAllExternalMeasurementsPerMonitorContainedIn(
                        variant = variant,
                        maxNumber = monitorSettings!!.measurementNumber,
                        top = visibleRegion.northeast.latitude,
                        bottom = visibleRegion.southwest.latitude,
                        left = visibleRegion.southwest.longitude,
                        right = visibleRegion.northeast.longitude
                    )
                }

                // clear previous grid if not visible anymore to improve map's performance
                if (!grid!!.isPointCovered(visibleRegion.center)) {
                    withContext(Dispatchers.Main) {
                        grid!!.clearGrid()
                        grid = MapGrid(visibleRegion.center, settings!!.gridUnitLength.toDouble())
                    }
                }
                grid!!.makeGrid(visibleRegion, measurements)
                withContext(Dispatchers.Main) {
                    grid!!.drawGrid(googleMap)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        locationTracker = LocationTracker(mainActivity)
        locationTracker!!.startLocationRecording()
        withContext(Dispatchers.IO) {
            settings = SettingsUtils.storedSettings
            monitorSettings = when(variant) {
                MonitorVariant.AUDIO -> settings!!.audio
                MonitorVariant.WIFI -> settings!!.wifi
                MonitorVariant.LTE -> settings!!.lte
            }

            val initialLatLng = LatLng(locationTracker!!.latitude, locationTracker!!.longitude)
            grid = MapGrid(initialLatLng, settings!!.gridUnitLength.toDouble())
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (initializing) {
            Text(text = "Initializing...")
        }
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    onCreate(null)
                    onResume()
                    getMapAsync { googleMap ->
                        initMap(googleMap)

                        googleMap.setOnCameraIdleListener {
                            renderMap(googleMap)
                        }
                    }
                }
            },
        )
    }
}

