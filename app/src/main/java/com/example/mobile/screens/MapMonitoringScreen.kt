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
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mobile.commons.LocationManager
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.MapMonitor.CurrentState.currentGridCell
import com.example.mobile.monitors.MapMonitor.CurrentState.currentLocation
import com.example.mobile.monitors.MonitorVariant
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



@Composable
fun LegendItem(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(1.dp) // Ancora meno padding per ogni voce
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)  // Ulteriore riduzione delle dimensioni del quadrato
                .background(color)
        )
        Spacer(modifier = Modifier.width(2.dp)) // Riduzione dello spazio tra quadrato e testo
        Text(name, fontSize = 8.sp) // Riduzione ulteriore delle dimensioni del testo
    }
}

@Composable
fun LegendView(gridUnitLength: Int,inUseMonitor: MonitorVariant, modifier: Modifier = Modifier) {
    val Orange = Color(0xFFFFA500) // ARGB per Arancione
    val Purple = Color(0xFF800080) // ARGB per Viola
    Column(
        modifier = Modifier
            .padding(4.dp) // Padding esterno
            .background(Color(0x66FFFFFF)) // Sfondo semitrasparente
            .padding(4.dp) // Padding interno
    ) {
        Text("Legenda:", fontWeight = FontWeight.Bold, fontSize = 10.sp)
        LegendItem("Nessun Dato", Color.Gray)
        LegendItem("Massimo", Color.Green)
        LegendItem("Alto", Orange)
        LegendItem("Medio", Color.Yellow)
        LegendItem("Basso", Purple)
        LegendItem("Minimo", Color.White)
        LegendItem("Invalido", Color.Black)

        Text("Dimensione delle celle: $gridUnitLength metri", fontSize = 8.sp)
        Text("Monitor in uso: $inUseMonitor", fontSize = 8.sp)
    }
}
@Composable
fun MapActivity (mapMonitor: MapMonitor,inUseMonitor: MonitorVariant){
    val context = LocalContext.current
    val gridUnitLengthState = remember { mutableIntStateOf(0) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val sharedGrid = remember { mutableStateOf<Map<String, List<Pair<Double, Double>>>?>(null) }
    var isMapCentered = remember { mutableStateOf(false) }
    val lastZoomLevel = remember { mutableStateOf(0f) }
    val lastBounds = remember { mutableStateOf(LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0))) }
    val readyToMonitor = remember { mutableStateOf(false) }
    val googleMapState = remember { mutableStateOf<GoogleMap?>(null) } // Memorizza lo stato di GoogleMap



    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Richiedi gli aggiornamenti sulla posizione qui
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }

    LaunchedEffect(key1 = true) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                hasLocationPermission = true
                val locationRequest = LocationRequest.create().apply {
                    interval = 10000
                    fastestInterval = 5000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                ).addOnFailureListener {
                    // Gestisci eventuali errori qui
                }
            }
            else -> {
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        // Carica le impostazioni
        val settings = withContext(Dispatchers.IO) {
            SettingsUtils.getStoredSettings()
        }
        gridUnitLengthState.value = settings.gridUnitLength
    }

    LaunchedEffect(key1 = inUseMonitor) {
        // Codice da eseguire quando inUseMonitor cambia
        println("Monitor in uso cambiato in: $inUseMonitor")
        isMapCentered.value = false

    }

// Assicurati di rimuovere gli aggiornamenti sulla posizione quando il composable non è più attivo
    DisposableEffect(key1 = true) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    if (hasLocationPermission && currentLocation != null ) {
        val currentLatLng = currentLocation!!
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        try {
                            onCreate(null)
                            onResume()
                            getMapAsync { googleMap ->
                                Log.d("MapActivity", "Map is ready")
                                googleMapState.value = googleMap // Memorizza il riferimento a googleMap nello stato

                                val gridUnit = gridUnitLengthState.value

                                // Centra la mappa solo se non è già stata centrata e se currentLatLng non è null
                                if (!isMapCentered.value && currentLatLng != null) {
                                    val initialZoomLevel = if (gridUnit == 10) 17f else 14f
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, initialZoomLevel))
                                    isMapCentered.value = true
                                }
                                googleMap.setOnCameraIdleListener {
                                    val currentZoomLevel = googleMap.cameraPosition.zoom
                                    val currentBounds = googleMap.projection.visibleRegion.latLngBounds

                                    if (currentZoomLevel != lastZoomLevel.value || currentBounds != lastBounds.value) {
                                        // Condizione aggiunta per evitare la rigenerazione delle griglie su spostamenti minori
                                        if (currentBounds != lastBounds.value || currentZoomLevel != lastZoomLevel.value) {
                                            val grid = mapMonitor.generateGrid(currentBounds, gridUnit)
                                            sharedGrid.value = grid
                                            readyToMonitor.value = true // Segnala che è pronto per monitorLocation

                                            Log.d("setonc", "grid = $grid" )
                                            Log.d("setonc", "sharedGrid = $sharedGrid" )

                                            mapMonitor.applyGridToMap(grid, googleMap, currentLatLng)
                                            lastZoomLevel.value = currentZoomLevel
                                            lastBounds.value = currentBounds
                                        }
                                    }
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
            LaunchedEffect(readyToMonitor.value) {
                if (readyToMonitor.value) {
                    sharedGrid.value?.let { grid ->

                        mapMonitor.monitorLocation(
                            map = googleMapState.value!!,
                            grid = sharedGrid.value!!,
                            fusedLocationClient = fusedLocationClient,
                            currentLatLng = currentLatLng
                        )
                        readyToMonitor.value = false // Resetta lo stato se necessario
                    }
                }
            }
            LegendView(gridUnitLengthState.value,inUseMonitor, modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp))

        }
    }

}

