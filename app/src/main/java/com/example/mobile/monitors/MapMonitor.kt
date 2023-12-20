package com.example.mobile.monitors

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import kotlin.math.cos

class MapMonitor {

    object CurrentState {
        var locationReceivedCalled: Boolean = false
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
            for ((cellName, square) in grid) {
                val polygonOptions = PolygonOptions().apply {
                    for (coord in square) {
                        add(LatLng(coord.first, coord.second))
                    }
                    strokeColor(Color.RED)
                    fillColor(Color.argb(50, 255, 0, 0))
                }
                map.addPolygon(polygonOptions)

                // Se hai bisogno di utilizzare cellName, puoi farlo qui
                // Ad esempio, potresti voler associare il nome della cella al poligono per riferimenti futuri
            }
        } catch (e: Exception) {
            Log.e("MapUtils", "Error adding grid to map", e)
        }
    }


    fun setupMap(latitude: Double, longitude: Double, map: GoogleMap, grid: Map<String, List<Pair<Double, Double>>>) {
        try {
            val initialLatLng = LatLng(latitude,longitude)
            CurrentState.currentLocationMarker = map.addMarker(MarkerOptions().position(initialLatLng).title("Current Location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 12f))
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
            if (currentLat >= cellCoordinates[0].first && currentLat <= cellCoordinates[2].first &&
                currentLon >= cellCoordinates[0].second && currentLon <= cellCoordinates[1].second) {
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
                val newCell =
                    findCurrentGridCell(location.latitude, location.longitude, grid)

                if (!CurrentState.locationReceivedCalled) {
                    onLocationReceived(location)
                    CurrentState.locationReceivedCalled = true
                    CurrentState.currentGridCell = newCell
                }
                updateLocation(location ,map)
                if (newCell != CurrentState.currentGridCell) {
                    CurrentState.currentGridCell = newCell
                    onLocationReceived(location)
                }
            }
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        } catch (ex: SecurityException) {
            // Gestire l'eccezione di sicurezza
        }
    }

    fun updateLocation(location: Location, map: GoogleMap) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        CurrentState.currentLocationMarker?.position = newLatLng
        map.animateCamera(CameraUpdateFactory.newLatLng(newLatLng))

        CurrentState.currentLocation = location
    }
}