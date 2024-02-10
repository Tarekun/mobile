package com.example.mobile.map

import android.graphics.Color
import com.example.mobile.database.Classification
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private fun computeAverageClassification(): Classification {
        //TODO:
        return if (classifications.isEmpty()) Classification.INVALID else classifications[0]
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

            //TODO: better handle of alpha
            val backgroundColor = when (computeAverageClassification()) {
                Classification.MIN -> Color.CYAN
                Classification.LOW -> Color.GREEN
                Classification.MEDIUM -> Color.BLACK
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