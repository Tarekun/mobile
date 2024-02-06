package com.example.mobile.monitors

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mobile.database.Classification
import com.example.mobile.database.DbManager
import com.example.mobile.monitors.MapMonitor.CurrentState.currentGridCell
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
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


    object CurrentState {
        var locationReceivedCalled: Boolean = false
        @Volatile
        var currentGridCell: String? = null
        var currentLocation: Location? = null
        var currentLocationMarker: Marker? = null
        var gridSystem: Map<String, List<Pair<Double, Double>>> = mapOf()
    }

    companion object {
        private val bounds = listOf(
            Pair(44.438739, 11.374712), // coord1
            Pair(44.512836, 11.442941), // coord2
            Pair(44.555109, 11.286599), // coord3
            Pair(44.472604, 11.234347)  // coord4
        )

        val minLat = bounds.minOf { it.first }
        val maxLat = bounds.maxOf { it.first }
        val minLon = bounds.minOf { it.second }
        val maxLon = bounds.maxOf { it.second }

        val stepLat = 1.0 / 111.0
        val avgLat = (minLat + maxLat) / 2
        val stepLon = 1.0 / (111.0 * cos(avgLat * (Math.PI / 180)))
    }
    fun generateGrid(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        stepLat: Double,
        stepLon: Double
    ): Map<String, List<Pair<Double, Double>>> {
        val grid = mutableMapOf<String, List<Pair<Double, Double>>>()

        var currentLat = minLat
        var cellId = 0
        while (currentLat < maxLat) {
            var currentLon = minLon
            while (currentLon < maxLon) {
                val square = listOf(
                    Pair(currentLat, currentLon),
                    Pair(currentLat + stepLat, currentLon),
                    Pair(currentLat + stepLat, currentLon + stepLon),
                    Pair(currentLat, currentLon + stepLon),
                    Pair(currentLat, currentLon) // Close the polygon
                )
                val cellName = "cell_${cellId++}"
                grid[cellName] = square
                currentLon += stepLon
            }
            currentLat += stepLat
        }
        CurrentState.gridSystem = grid
        return grid
    }


    private fun addGridToMap(grid: Map<String, List<Pair<Double, Double>>>, map: GoogleMap) {
        try {
            Log.d("MapUtils", "inside addGridToMap" )
            for ((cellName, square) in grid) {
                val polygonOptions = PolygonOptions().apply {
                    for (coord in square) {
                        add(LatLng(coord.first, coord.second))
                    }
                    strokeColor(Color.RED)
                    fillColor(Color.argb(50, 255, 0, 0))
                }
                val polygon = map.addPolygon(polygonOptions)
                cellPolygons[cellName] = polygon // Memorizza il riferimento al poligono
            }
            colorCurrentGrid(MonitorVariant.AUDIO, currentGridCell)

        } catch (e: Exception) {
            Log.e("MapUtils", "Error adding grid to map", e)
        }
    }


    fun setupMap(latitude: Double, longitude: Double, map: GoogleMap, grid: Map<String, List<Pair<Double, Double>>>) {
        try {
            val initialLatLng = LatLng(latitude, longitude)
            CurrentState.currentLocationMarker = map.addMarker(MarkerOptions().position(initialLatLng).title("Current Location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 15f)) // Aumenta il livello di zoom qui
            Log.d("setupMap", "currentgrid= $currentGridCell" )
            addGridToMap(grid, map)
        } catch (e: Exception) {
            Log.e("MapUtils", "Error setting up map", e)
        }
    }


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
        grid: Map<String, List<Pair<Double, Double>>>,
        map: GoogleMap,
        onLocationReceived: (Location) -> Unit,
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d("onlocationChanged", "inside onlocationChanged" )
                Log.d("onlocationChanged", "$location" )
                val newCell = findCurrentGridCell(location.latitude, location.longitude, grid)
                Log.d("onlocationChanged", "newCell= $newCell" )

                if (!CurrentState.locationReceivedCalled) {
                    onLocationReceived(location)
                    CurrentState.locationReceivedCalled = true
                    CurrentState.currentGridCell = newCell
                    Log.d("MonitorLocation", "currentgrid= $currentGridCell" )
                }
                updateLocation(location ,map)
                if (newCell != CurrentState.currentGridCell) {
                    CurrentState.currentGridCell = newCell
                    onLocationReceived(location)
                }
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
            Log.d("MapActivity", "inside catch")
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
        Log.d("MapUtils", "inside colorCurrentGrid" )
        coroutineScope.launch {
            val lastSignalStrengthList = withContext(Dispatchers.IO) {
                DbManager.lastSignalStrength(currentGrid, monitorType)
            }
            val averageSignalStrength = calculateAverage(lastSignalStrengthList)
            val classification = classifySignalStrength((averageSignalStrength))
            Log.d("MapUtils", "classification = $classification" )
            Log.d("MapUtils", "currentgrid= $currentGrid" )

            val color = when (classification) {
                Classification.NO_DATA -> Color.GRAY
                Classification.MAX -> Color.GREEN
                Classification.HIGH -> Color.argb(50, 255, 165, 0) // Arancione
                Classification.MEDIUM -> Color.YELLOW
                Classification.LOW -> Color.argb(50, 128, 0, 128) // Viola
                Classification.MIN -> Color.WHITE
                Classification.INVALID -> Color.BLACK
            }
            Log.d("MapUtils", "color= $color" )

            withContext(Dispatchers.Main) {
                Log.d("MapUtils", "inside dispatchermain" )
                // Aggiorna il colore della griglia corrente
                currentGrid?.let { gridName ->
                    cellPolygons[gridName]?.fillColor = color
                    Log.d("MapUtils", "color changed" )
                }
            }
        }
    }



    fun calculateAverage(signalStrengthList: List<Double>): Double {
        if (signalStrengthList.isEmpty()) {
            return Double.NaN // Restituisce un valore Double che rappresenta "INVALID"
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

}