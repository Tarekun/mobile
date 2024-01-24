package com.example.mobile.screens
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mobile.monitors.MapMonitor
import com.google.android.gms.maps.MapView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


@Composable
fun MapActivity (){


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mapMonitor by remember { mutableStateOf(MapMonitor(context,coroutineScope)) }
    // Generate the grid
    val grid = mapMonitor.generateGrid(
        MapMonitor.minLat,
        MapMonitor.maxLat,
        MapMonitor.minLon,
        MapMonitor.maxLon,
        MapMonitor.stepLat,
        MapMonitor.stepLon
    )

    // Stato per tracciare se il permesso di localizzazione è stato concesso
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Launcher per la richiesta del permesso di localizzazione
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Permesso concesso, continua con le operazioni di localizzazione
        } else {
            // Permesso negato, gestisci di conseguenza
        }
    }

    // Controlla se il permesso di localizzazione è stato già concesso
    LaunchedEffect(key1 = true) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            // Richiedi il permesso di localizzazione
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (hasLocationPermission) {
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

}

