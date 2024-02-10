package com.example.mobile.map

import android.util.Log
import com.example.mobile.database.Measurement
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.cos

fun moveLatitude(baseLatitude: Double, distanceMeters: Double): Double {
    val earthRadiusMeters = 6_371_000.0
    val distanceDegreesLatitude = (distanceMeters / earthRadiusMeters) * (180 / Math.PI)

    val newLatitude = baseLatitude + distanceDegreesLatitude
    return newLatitude
}

fun moveLongitude(baseLongitude: Double, distanceMeters: Double, referenceLatitude: Double): Double {
    val metersPerDegree = 111_320
    // Convert latitude to radians for the cosine calculation
    val latitudeRadians = Math.toRadians(referenceLatitude)
    val distanceDegreesLongitude = distanceMeters / (metersPerDegree * cos(latitudeRadians))
    return baseLongitude + distanceDegreesLongitude
}

class MapGrid(val center: LatLng, private val squareSideMeters: Double) {
    private val squares: MutableMap<SquareIndex, Square> = mutableMapOf()
    private var coveredArea: LatLngBounds
    private var lastSquare: Square
    private var squaresToDraw: MutableList<Square> = mutableListOf()

    private var rowsLength: Int
    private var columnsLength: Int
    private var topLeftIndex: SquareIndex
    private var topRightIndex: SquareIndex
    private var bottomLeftIndex: SquareIndex
    private var bottomRightIndex: SquareIndex

    init {
        val halfStep = squareSideMeters / 2
        val firstSquare = Square(
            top = moveLatitude(center.latitude, halfStep),
            bottom = moveLatitude(center.latitude, -halfStep),
            right = moveLongitude(center.longitude, halfStep, center.latitude),
            left = moveLongitude(center.longitude, -halfStep, center.latitude),
            index = SquareIndex(0, 0),
        )
        val firstIndex = SquareIndex(0, 0)
        squares[firstIndex] = firstSquare
        squaresToDraw.add(firstSquare)
        lastSquare = firstSquare
        coveredArea = LatLngBounds(firstSquare.bottomLeft, firstSquare.topRight)

        rowsLength = 1
        columnsLength = 1
        topLeftIndex = firstIndex
        topRightIndex = firstIndex
        bottomLeftIndex = firstIndex
        bottomRightIndex = firstIndex
    }

    private fun findContainingSquare(latitude: Double, longitude: Double): Square? {
        //TODO: this can now be done in constant time making a conversion from coordinates to (hypotetical) indexes
        for ((_, square) in squares) {
            if (square.contains(latitude, longitude)) {
                return square
            }
        }
        return null
    }

    private fun makeSquare(
        top: Double,
        bottom: Double,
        left: Double,
        right: Double,
        index: SquareIndex
    ): Square {
        val newSquare = Square(
            bottom = bottom,
            top = top,
            left = left,
            right = right,
            index = index
        )
        squares[index] = newSquare
        squaresToDraw.add(newSquare)
        lastSquare = newSquare
        return newSquare
    }

    private fun isAreaCovered(areaToCover: LatLngBounds): Boolean {
        val southeast = LatLng(areaToCover.southwest.latitude, areaToCover.northeast.longitude)
        val northwest = LatLng(areaToCover.northeast.latitude, areaToCover.southwest.longitude)
        return coveredArea.contains(areaToCover.northeast) && coveredArea.contains(northwest) && coveredArea.contains(southeast) && coveredArea.contains(areaToCover.southwest)
    }

    private fun addHorizontalSegment(direction: Int) {
        Log.d("mio", "ORIZZONTALE $direction")
        val baseIndex = if (direction == 1) topLeftIndex else bottomLeftIndex
        for (i in 0 until rowsLength) {
            // add i to x to move horizontally
            val adjacentSquare = squares[baseIndex.copy(x = baseIndex.x + i)]
            // adds direction to place the new square next to the previous one
            val newIndex = baseIndex.copy(x = baseIndex.x + i, y = baseIndex.y + direction)

            val left = adjacentSquare!!.left
            val right = adjacentSquare.right
            val bottom =
                if (direction == 1) adjacentSquare.top
                else moveLatitude(adjacentSquare.bottom, -squareSideMeters)
            val top =
                if (direction == 1) moveLatitude(adjacentSquare.top, squareSideMeters)
                else adjacentSquare.bottom
            val newSquare = makeSquare(top = top, bottom = bottom, right = right, left = left, index = newIndex)
        }

        columnsLength++
        if (direction == 1) {
            topLeftIndex = topLeftIndex.copy(y = topLeftIndex.y + direction)
            topRightIndex = topRightIndex.copy(y = topRightIndex.y + direction)

            val newNorthEast = LatLng(
                lastSquare.top,
                coveredArea.northeast.longitude
            )
            coveredArea = LatLngBounds(coveredArea.southwest, newNorthEast)
        }
        else {
            bottomLeftIndex = bottomLeftIndex.copy(y = bottomLeftIndex.y + direction)
            bottomRightIndex = bottomRightIndex.copy(y = bottomRightIndex.y + direction)

            val newSouthWest = LatLng(
                lastSquare.bottom,
                coveredArea.southwest.longitude
            )
            coveredArea = LatLngBounds(newSouthWest, coveredArea.northeast)
        }
    }

    private fun addVerticalSegment(direction: Int) {
        Log.d("mio", "VERTICALE $direction")
        val baseIndex = if (direction == 1) bottomRightIndex else bottomLeftIndex
        for (j in 0 until columnsLength) {
            val adjacentSquare = squares[baseIndex.copy(y = baseIndex.y + j)]
            val newIndex = baseIndex.copy(x = baseIndex.x + direction, y = baseIndex.y + j)

            val bottom = adjacentSquare!!.bottom
            val top = adjacentSquare.top
            val left =
                if (direction == 1) adjacentSquare.right
                else moveLongitude(adjacentSquare.left, -squareSideMeters, top)
            val right =
                if (direction == 1) moveLongitude(adjacentSquare.right, squareSideMeters, top)
                else adjacentSquare.left
            val newSquare = makeSquare(top = top, bottom = bottom, right = right, left = left, index = newIndex)
        }

        rowsLength++
        if (direction == 1) {
            bottomRightIndex = bottomRightIndex.copy(x = bottomRightIndex.x + direction)
            topRightIndex = topRightIndex.copy(x = topRightIndex.x + direction)

            val newNorthEast = LatLng(
                coveredArea.northeast.latitude,
                lastSquare.right
            )
            coveredArea = LatLngBounds(coveredArea.southwest, newNorthEast)
        }
        else {
            bottomLeftIndex = bottomLeftIndex.copy(x = bottomLeftIndex.x + direction)
            topLeftIndex = topLeftIndex.copy(x = topLeftIndex.x + direction)

            val newSouthWest = LatLng(
                coveredArea.southwest.latitude,
                lastSquare.left
            )
            coveredArea = LatLngBounds(newSouthWest, coveredArea.northeast)
        }

    }

    fun makeGrid(areaToCover: LatLngBounds, measurements: List<Measurement>) {
        var n = 0
        while(!isAreaCovered(areaToCover)) {
            val leftDistance = coveredArea.southwest.longitude - areaToCover.southwest.longitude
            val rightDistance = areaToCover.northeast.longitude - coveredArea.northeast.longitude
            val bottomDistance = coveredArea.southwest.latitude - areaToCover.southwest.latitude
            val topDistance = areaToCover.northeast.latitude - coveredArea.northeast.latitude

            when (maxOf(leftDistance, bottomDistance, topDistance, rightDistance)) {
                leftDistance -> addVerticalSegment(-1)
                rightDistance -> addVerticalSegment(1)
                topDistance -> addHorizontalSegment(1)
                bottomDistance -> addHorizontalSegment(-1)
            }

            n++
        }
        countClassifications(measurements)
    }

    private fun countClassifications(measurements: List<Measurement>) {
        for (measurement in measurements) {
            val containingSquare = findContainingSquare(measurement.latitude, measurement.longitude)
            if (containingSquare != null) {
                containingSquare.addClassification(measurement.classification)
            }
        }
    }

    fun drawGrid(googleMap: GoogleMap) {
        while(squaresToDraw.isNotEmpty()) {
            val square = squaresToDraw.removeFirst()
            square.draw(googleMap)
        }
    }

    fun clearGrid() {
        for ((index, square) in squares) {
            square.clear()
        }
    }

    fun isPointCovered(point: LatLng): Boolean {
        return coveredArea.contains(point)
    }
}