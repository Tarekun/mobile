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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
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
fun LegendView(gridUnitLength: Int, modifier: Modifier = Modifier) {
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
    }
}
@Composable
fun MapActivity (mapMonitor: MapMonitor){
    val context = LocalContext.current
    val gridUnitLengthState = remember { mutableIntStateOf(0) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var sharedGrid  by remember {mutableStateOf<Map<String, List<Pair<Double, Double>>>?>(null)}
    val monitor


    // Funzione per aggiornare la posizione attuale
    fun updateCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            currentLocation = location?.let { LatLng(it.latitude, it.longitude) }
        }.addOnFailureListener {
            // Gestione dell'errore, ad esempio impostando una posizione predefinita
            currentLocation = LatLng(0.0, 0.0) // Posizione predefinita
        }
    }

    // Launcher per la richiesta del permesso di localizzazione
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Ottieni la posizione attuale solo se il permesso è stato concesso
            updateCurrentLocation()
        }
    }

    LaunchedEffect(key1 = Unit) {
        // Controlla se i permessi sono già stati concessi
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            // Ottieni la posizione attuale
            updateCurrentLocation()
        } else {
            // Richiedi il permesso di localizzazione
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Carica le impostazioni dall'IO
        val settings = withContext(Dispatchers.IO) {
            SettingsUtils.getStoredSettings()
        }
        // Aggiorna lo stato sul thread principale
        gridUnitLengthState.value = settings.gridUnitLength
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

                                // Ottieni la posizione corrente da un gestore di localizzazione o una fonte affidabile

                                val gridUnit = gridUnitLengthState.value
                                Log.d("MapActivity", "gridUnit:$gridUnit")

                                // Imposta il livello di zoom iniziale e sposta la camera sulla posizione corrente
                                val initialZoomLevel = 14f // Scegli un livello di zoom iniziale adeguato
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, initialZoomLevel))

                                // Inizializza lastZoomLevel con il livello di zoom iniziale
                                var lastZoomLevel = initialZoomLevel
                                Log.d("MapActivity", "zoom:$lastZoomLevel")

                                googleMap.setOnCameraIdleListener {
                                    // Questo listener viene chiamato dopo che la camera ha finito di muoversi e lo zoom è stato applicato
                                    val lastZoomLevel = googleMap.cameraPosition.zoom
                                    val mapBounds = googleMap.projection.visibleRegion.latLngBounds
                                    Log.d("MapActivity", "Zoom after moveCamera: $lastZoomLevel")
                                    Log.d("MapActivity", "Bounds after zoom: $mapBounds")

                                    // Genera la griglia basata sui bounds attuali
                                    val grid = mapMonitor.generateGrid(mapBounds, gridUnit)
                                    sharedGrid = grid
                                    mapMonitor.applyGridToMap(grid,googleMap, currentLatLng)
                                }

                                mapMonitor.monitorLocation(
                                    context = context,
                                    map = googleMap,
                                    grid = sharedGrid ?: emptyMap()
                                ) {
                                    mapMonitor.colorCurrentGrid(MonitorVariant.AUDIO, currentGridCell)

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
            LegendView(gridUnitLengthState.value, modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp))

        }
    }

}

