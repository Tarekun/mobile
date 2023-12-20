package com.example.mobile.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mobile.monitors.MapMonitor
import com.google.android.gms.maps.MapView


@Composable
fun MapActivity (){
    val mapMonitor by lazy {
        MapMonitor()
    }
    // Generate the grid
    val grid = mapMonitor.generateGrid(
        MapMonitor.minLat,
        MapMonitor.maxLat,
        MapMonitor.minLon,
        MapMonitor.maxLon,
        MapMonitor.stepLat,
        MapMonitor.stepLon
    )

    AndroidView(
        factory = { context ->
            MapView(context).apply {
                try {
                    onCreate(null)
                    onResume()
                    getMapAsync { googleMap ->
                        Log.d("MapActivity", "Map is ready")
                        mapMonitor.monitorLocation(
                            context = context,
                            grid = grid,
                            map = googleMap
                        ) { location ->
                            Log.d("MapActivity", "Map setting up")
                            mapMonitor.setupMap(location.latitude, location.longitude, googleMap, grid)
                        }

                    }

                } catch (e: Exception) {
                    Log.e("MapActivity", "Error initializing map view", e)
                }
            }
        },
        update = { mapView ->
            // Aggiornamenti della mappa, se necessario
        },
        modifier = Modifier.fillMaxSize() // Ensure the map takes up the full size of the composable
    )
}

