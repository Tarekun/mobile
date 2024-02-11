package com.example.mobile.map

import android.graphics.Color
import com.example.mobile.database.Classification
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import kotlin.math.roundToInt

data class SquareIndex(val x: Int, val y: Int)

class Square(
    val top: Double,
    val bottom: Double,
    val left: Double,
    val right: Double,
    val index: SquareIndex
) {
    var polygon: Polygon? = null
    private val classifications: MutableList<Classification> = mutableListOf()
    private val bounds: LatLngBounds = LatLngBounds(bottomLeft, topRight)
    private val wasDrawn: Boolean
        get() = polygon != null

    val topLeft: LatLng
        get() = LatLng(top, left)
    val topRight: LatLng
        get() = LatLng(top, right)
    val bottomLeft: LatLng
        get() = LatLng(bottom, left)
    val bottomRight: LatLng
        get() = LatLng(bottom, right)
    val center: LatLng
        get() = bounds.center

    private fun computeAverageClassification(): Classification {
        if (classifications.isEmpty()) return Classification.INVALID

        var sum = 0
        for (classification in classifications) {
            sum += classification.intValue
        }
        val average = sum.toDouble() / classifications.size
        return when(average.roundToInt()) {
            4 -> Classification.MAX
            3 -> Classification.HIGH
            2 -> Classification.MEDIUM
            1 -> Classification.LOW
            0 -> Classification.MIN
            else -> Classification.INVALID
        }
    }

    private fun createOptions(): PolygonOptions {
        return PolygonOptions().apply {
            add(topLeft)
            add(topRight)
            add(bottomRight)
            add(bottomLeft)
            //fifth one closes the square
            add(topLeft)
            strokeWidth(3f)

            val backgroundColor = when (computeAverageClassification()) {
                Classification.MIN -> Color.CYAN
                Classification.LOW -> Color.GREEN
                // orange color
                Classification.MEDIUM -> Color.rgb(255, 165, 0)
                Classification.HIGH -> Color.YELLOW
                Classification.MAX -> Color.RED
                else -> null
            }
            backgroundColor?.let {
                fillColor(
                    Color.argb(
                        64, // Adjust alpha for transparency
                        Color.red(it),
                        Color.green(it),
                        Color.blue(it)
                    )
                )
            }
        }
    }

    fun contains(latitude: Double, longitude: Double): Boolean {
        return bounds.contains(LatLng(latitude, longitude))
    }

    fun addClassification(classification: Classification) {
        classifications.add(classification)
    }

    fun draw(googleMap: GoogleMap) {
        if (!wasDrawn) {
            val polygonOptions = createOptions()
            // adding to the local polygon remembers that this was drawn
            polygon = googleMap.addPolygon(polygonOptions)
        }
    }

    fun clear() {
        if (wasDrawn) {
            polygon!!.remove()
        }
    }
}