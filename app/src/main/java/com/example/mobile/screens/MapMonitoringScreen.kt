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
import com.example.mobile.database.SettingsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun MapActivity (mapMonitor: MapMonitor){
    val context = LocalContext.current
    val gridUnitLengthState = remember { mutableIntStateOf(0) } // Inizializzato con un valore di default
    var hasLocationPermission by remember { mutableStateOf(false) }
    val Orange = Color(0xFFFFA500) // ARGB per Arancione
    val Purple = Color(0xFF800080) // ARGB per Viola


    // Launcher per la richiesta del permesso di localizzazione
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        // Altre azioni in caso di concessione o negazione del permesso
    }

    LaunchedEffect(key1 = true) {
        // Controlla e richiede i permessi
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Esegui le operazioni del database nel contesto IO
        withContext(Dispatchers.IO) {
            val settings = SettingsUtils.getStoredSettings()
            withContext(Dispatchers.Main) {
                // Aggiorna lo stato sul thread principale
                gridUnitLengthState.value = settings.gridUnitLength
            }
        }
    }

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




    if (hasLocationPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                                    map = googleMap,
                                    gridUnit = gridUnitLengthState.value
                                ) { location, grid ->
                                    Log.d("Anndroidview", "Map setting up")
                                    //Log.d("Anndroidview", "currentgrid= ${MapMonitor.CurrentState.currentGridCell}" )
                                    mapMonitor.setupMap(
                                        location.latitude,
                                        location.longitude,
                                        googleMap,
                                        grid
                                    )
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

