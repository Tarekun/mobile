package com.example.mobile.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.mobile.commons.LocationManager
import com.example.mobile.database.Measurement
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.MonitorSettings
import com.example.mobile.database.SettingsTable
import com.example.mobile.database.SettingsUtils
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
) {
    val coroutineScope = rememberCoroutineScope()
    var initializing by remember { mutableStateOf(true) }
    var grid: MapGrid? by remember { mutableStateOf(null) }
    var settings: SettingsTable? by remember { mutableStateOf(null) }
    var monitorSettings: MonitorSettings? by remember { mutableStateOf(null) }

    fun initMap(googleMap: GoogleMap) {
        val initialLatLng = LatLng(LocationManager.latitude, LocationManager.longitude)
        googleMap.addMarker(MarkerOptions().position(initialLatLng))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 20f))

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
                    grid!!.clearGrid()
                    grid = MapGrid(visibleRegion.center, settings!!.gridUnitLength.toDouble())
                }
                grid!!.makeGrid(visibleRegion, measurements)
                withContext(Dispatchers.Main) {
                    grid!!.drawGrid(googleMap)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            settings = SettingsUtils.storedSettings
            monitorSettings = when(variant) {
                MonitorVariant.AUDIO -> settings!!.audio
                MonitorVariant.WIFI -> settings!!.wifi
                MonitorVariant.LTE -> settings!!.lte
            }

            val initialLatLng = LatLng(LocationManager.latitude, LocationManager.longitude)
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
//                modifier = Modifier.fillMaxSize()
        )
    }
}

