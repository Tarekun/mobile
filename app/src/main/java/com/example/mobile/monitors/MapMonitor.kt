package com.example.mobile.monitors

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.MapMonitor.CurrentState.currentGridCell
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

//    private val dbManager = DbManager(context)
    val cellPolygons = mutableMapOf<String, Polygon>()


    // Generate the grid

    object CurrentState {
        var locationReceivedCalled: Boolean = false
        @Volatile
        var currentGridCell: String? = null
        var currentLocation: Location? = null
        var currentLocationMarker: Marker? = null
        var gridSystem: Map<String, List<Pair<Double, Double>>> = mapOf()
    }

    /*fun generateGrid(
        currentLat: Double,
        currentLon: Double,
        numCellsPerSide: Int,
        gridUnit: Int // Dimensione della cella in metri
    ): Map<String, List<Pair<Double, Double>>> {
        val grid = mutableMapOf<String, List<Pair<Double, Double>>>()

        val metersPerDegree = 111000.0 // approssimativamente 111 km per grado
        val stepLat = gridUnit.toDouble() / metersPerDegree
        val stepLon = stepLat / cos(currentLat * (Math.PI / 180))

        // Sposta l'inizio di mezza cella a sinistra e mezza cella in su
        val startLat = currentLat - (numCellsPerSide / 2) * stepLat + (stepLat / 2)
        val startLon = currentLon - (numCellsPerSide / 2) * stepLon + (stepLon / 2)

        var cellLat = startLat
        var cellId = 0
        for (i in 0 until numCellsPerSide) {
            var cellLon = startLon
            for (j in 0 until numCellsPerSide) {
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
    }*/





    /*private fun addGridToMap(grid: Map<String, List<Pair<Double, Double>>>, map: GoogleMap) {
        try {
            Log.d("MapUtils", "inside addGridToMap" )
            for ((cellName, square) in grid) {
                val polygonOptions = PolygonOptions().apply {
                    for (coord in square) {
                        add(LatLng(coord.first, coord.second))
                    }
                    strokeColor(Color.RED)
                    //fillColor(Color.argb(50, 255, 0, 0))
                }
                val polygon = map.addPolygon(polygonOptions)
                cellPolygons[cellName] = polygon // Memorizza il riferimento al poligono
            }
            colorCurrentGrid(MonitorVariant.AUDIO, currentGridCell)

        } catch (e: Exception) {
            Log.e("MapUtils", "Error adding grid to map", e)
        }
    }
*/




    fun findCurrentGridCell(
        currentLat: Double,
        currentLon: Double,
        grid: Map<String, List<Pair<Double, Double>>>
    ): String? {
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
        context: Context,
        map: GoogleMap,
        grid: Map<String, List<Pair<Double, Double>>>,
        onLocationReceived: () -> Unit,
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val newCell = findCurrentGridCell(location.latitude, location.longitude, grid)

                if (newCell != currentGridCell) {
                    currentGridCell = newCell
                }
                updateLocation(location ,map)
                onLocationReceived()
            }
        }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                50f,
                locationListener
            )
        } catch (ex: SecurityException) {
            Log.e("MapActivity", "Security Exception: ${ex.message}",ex)
        }
    }

    fun updateLocation(location: Location, map: GoogleMap) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        CurrentState.currentLocationMarker?.position = newLatLng
        map.animateCamera(CameraUpdateFactory.newLatLng(newLatLng))

        CurrentState.currentLocation = location
    }

    fun colorCurrentGrid(monitorType: MonitorVariant, currentGrid: String?) {
        coroutineScope.launch {
            val lastSignalStrengthList = withContext(Dispatchers.IO) {
                DbManager.lastSignalStrength(currentGrid, monitorType)
            }
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
                // Aggiorna il colore della griglia corrente
                currentGrid?.let { gridName ->
                    cellPolygons[gridName]?.fillColor = color
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
        // Pulisci la mappa da vecchie griglie
        googleMap.clear()
        val newCell = findCurrentGridCell(currentLocation.latitude, currentLocation.longitude, grid)
        currentGridCell = newCell
        googleMap.addMarker(MarkerOptions().position(currentLocation).title("Qui sono io!"))

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
        colorCurrentGrid(MonitorVariant.AUDIO, currentGridCell )

        // Altre operazioni di styling o aggiunte sulla mappa
    }




}