package com.example.mobile.monitors

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.MapMonitor.CurrentState.currentGridCell
import com.example.mobile.monitors.MapMonitor.CurrentState.oldCell
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos

class MapMonitor(context: Context, private val coroutineScope: CoroutineScope) {

    val cellPolygons = mutableMapOf<String, Polygon>()

    object CurrentState {
        @Volatile
        var currentGridCell: String? = null
        var currentLocation: Location? = null
        var currentLocationMarker: Marker? = null
        var gridSystem: Map<String, List<Pair<Double, Double>>> = mapOf()
        var oldCell: String? = null
    }

    fun findCurrentGridCell(
        currentLat: Double,
        currentLon: Double,
        grid: Map<String, List<Pair<Double, Double>>>

    ): String? {
        Log.d("findc", "currentLat = $currentLat" )
        Log.d("findc", "currentLon = $currentLon" )
        Log.d("findc", "grid = $grid" )
        grid.forEach { (cellName, cellCoordinates) ->
            val bottomLeft = cellCoordinates[0]  // cellCoordinates[0] è il punto in basso a sinistra
            val topRight = cellCoordinates[2]    // cellCoordinates[2] è il punto in alto a destra

            if (currentLat >= bottomLeft.first && currentLat < topRight.first &&
                currentLon >= bottomLeft.second && currentLon < topRight.second) {
                return cellName // Restituisce il nome della cella corrente
            }
        }
        return null // Nessuna cella corrisponde
    }


    fun monitorLocation(
        map: GoogleMap,
        grid: Map<String, List<Pair<Double, Double>>>,
        fusedLocationClient: FusedLocationProviderClient,
        currentLatLng: LatLng,
    ) {

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    //Log.d("monitorLOc", "sharedGrid = $grid" )

                    val location = locationResult.locations.last()
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    val newCell = findCurrentGridCell(currentLatLng.latitude, currentLatLng.longitude, grid)
                    Log.d("findcurr", "newCell = $newCell" )

                    if (newCell != currentGridCell && newCell!= null) {
                        Log.d("ATTIVAAAAAA", "ATTIVAAAAAA" )

                        oldCell = currentGridCell

                        Log.d("gridchange", "oldGridCell = $currentGridCell" )
                        Log.d("gridchange", "newCell = $newCell" )

                        currentGridCell = newCell
                        colorCurrentGrid(MonitorVariant.AUDIO, currentGridCell, oldCell)
                        updateLocation(location, map)
                    }
                    // Aggiorna la posizione del marker senza recentrare la mappa
                    CurrentState.currentLocationMarker?.let {
                        it.position = newLatLng
                    } ?: run {
                        CurrentState.currentLocationMarker = map.addMarker(MarkerOptions().position(newLatLng).title("Qui sono io!"))
                    }

                }
            }
        }


        val locationRequest = LocationRequest.create().apply {
            interval = 5000L
            fastestInterval = 2500L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (ex: SecurityException) {
            Log.e("MapActivity", "Security Exception: ${ex.message}", ex)
        }
    }



    fun updateLocation(location: Location, map: GoogleMap) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        CurrentState.currentLocationMarker?.position = newLatLng

        CurrentState.currentLocation = location
    }

    fun colorCurrentGrid(monitorType: MonitorVariant, currentGrid: String?, oldCell: String?) {
        coroutineScope.launch {
            val lastSignalStrengthList = withContext(Dispatchers.IO) {
                DbManager.lastSignalStrength(currentGrid, monitorType)
            }
            //Log.d("ATTIVATAAAAAAAA", "ATTIVATAAAAAAAA" )

            val averageSignalStrength = calculateAverage(lastSignalStrengthList)
            val classification = classifySignalStrength((averageSignalStrength))
            //Log.d("MapUtils", "classification = $classification" )
            //Log.d("MapUtils", "currentgrid= $currentGrid" )

            val alpha = 128 // Imposta un valore alfa per la trasparenza (0 = completamente trasparente, 255 = completamente opaco)

            val color = when (classification) {
                Classification.NO_DATA -> Color.argb(alpha, 128, 128, 128) // Grigio
                Classification.MAX -> Color.argb(alpha, 0, 255, 0) // Verde
                Classification.HIGH -> Color.argb(alpha, 255, 165, 0) // Arancione
                Classification.MEDIUM -> Color.argb(alpha, 255, 255, 0) // Giallo
                Classification.LOW -> Color.argb(alpha, 128, 0, 128) // Viola
                Classification.MIN -> Color.argb(alpha, 255, 255, 255) // Bianco
                Classification.INVALID -> Color.argb(alpha, 0, 0, 0) // Nero
            }

            withContext(Dispatchers.Main) {
                //oldCell?.let { prevGridName ->
                    //Log.d("coloroldcell", "olfcell = ${CurrentState.oldCell}" )

                   // cellPolygons[prevGridName]?.fillColor = Color.argb(0, 0, 0, 0) // Viola
                //}?: Log.e("vecchia", "Poligono non trovato per la griglia: $oldCell")
                currentGrid?.let { gridName ->
                    //Log.d("coloroldcell", "newcell = $currentGrid" )

                    cellPolygons[gridName]?.let { polygon ->
                        polygon.fillColor = color
                    } ?: Log.e("nuova", "Poligono non trovato per la griglia: $gridName")
                }
            }
        }
    }

    fun calculateAverage(signalStrengthList: List<Double>): Double {
        if (signalStrengthList.isEmpty()) {
            return Double.NaN // Restituisce un valore Double che rappresenta "NO_DATA"
        }

        val sum = signalStrengthList.sum()
        return sum / signalStrengthList.size
    }

    fun classifySignalStrength(dB: Double): Classification {
        if (dB.isNaN()) {
            return Classification.NO_DATA // Nessuna misurazione trovata
        }

        return when (dB) {
            in -3.0..0.0 -> Classification.MAX
            in -24.0..-3.0 -> Classification.HIGH
            in -40.0..-24.0 -> Classification.MEDIUM
            in -60.0..-40.0 -> Classification.LOW
            in Double.NEGATIVE_INFINITY..-60.0 -> Classification.MIN
            else -> Classification.INVALID
        }
    }

    fun generateGrid(
        mapBounds: LatLngBounds, // Confine della mappa visualizzata
        gridUnit: Int // Dimensione della cella in metri
    ): Map<String, List<Pair<Double, Double>>> {
        val grid = mutableMapOf<String, List<Pair<Double, Double>>>()

        val metersPerDegree = 111000.0 // approssimativamente 111 km per grado
        val stepLat = gridUnit.toDouble() / metersPerDegree
        val stepLon = stepLat / cos(mapBounds.center.latitude * (Math.PI / 180))

        val startLat = mapBounds.southwest.latitude
        val startLon = mapBounds.southwest.longitude
        val endLat = mapBounds.northeast.latitude
        val endLon = mapBounds.northeast.longitude

        var cellLat = startLat
        var cellId = 0
        while (cellLat < endLat) {
            var cellLon = startLon
            while (cellLon < endLon) {
                val square = listOf(
                    Pair(cellLat, cellLon),
                    Pair(cellLat + stepLat, cellLon),
                    Pair(cellLat + stepLat, cellLon + stepLon),
                    Pair(cellLat, cellLon + stepLon),
                    Pair(cellLat, cellLon) // Close the polygon
                )
                val cellName = "cell_${cellId++}"
                grid[cellName] = square
                cellLon += stepLon
            }
            cellLat += stepLat
        }

        CurrentState.gridSystem = grid
        return grid
    }

     fun applyGridToMap(
        grid: Map<String, List<Pair<Double, Double>>>,
        googleMap: GoogleMap,
        currentLocation:  LatLng
    ) {

         // Rimuovi solo i poligoni delle griglie precedenti
         cellPolygons.values.forEach { it.remove() }
         cellPolygons.clear() // Pulisci la mappa dei poligoni per le future aggiunte
        val newCell = findCurrentGridCell(currentLocation.latitude, currentLocation.longitude, grid)
        currentGridCell = newCell

        // Aggiungi le celle della griglia sulla mappa
        grid.forEach { (cellName, square) ->
            val polygonOptions = PolygonOptions()
            square.forEach { (lat, lon) ->
                polygonOptions.add(LatLng(lat, lon))
            }
            polygonOptions.strokeColor(Color.RED)
            // Puoi aggiungere altri stili al poligono se necessario

            val polygon = googleMap.addPolygon(polygonOptions)
            cellPolygons[cellName] = polygon // Memorizza il riferimento al poligono
        }

        // Colora la cella corrente (se applicabile)
         Log.d("gengrid", "olfcell = $oldCell" )

         colorCurrentGrid(MonitorVariant.AUDIO, currentGridCell, oldCell )

        // Altre operazioni di styling o aggiunte sulla mappa
    }




}