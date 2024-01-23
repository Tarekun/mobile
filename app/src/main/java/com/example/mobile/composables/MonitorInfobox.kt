package com.example.mobile.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.database.MonitorSettings
import com.example.mobile.monitors.MonitorState
import com.example.mobile.monitors.MonitorVariant
import kotlin.math.roundToInt

@Composable
fun MonitorInfobox(
    variant: MonitorVariant,
    monitorStatus: MonitorState,
    monitorSettings: MonitorSettings,
    currentValue: Double,
    measurementsNumber: Int,
    externalMeasurementsNumber: Int,
) {

    val twoDecimalValue = (currentValue * 100.0).roundToInt() / 100.0
    val firstColumnLength = 200.dp
    //to avoid duplicate code
    val infoLabels = mapOf<String, String>(
        Pair("Selected monitor:", variant.name),
        Pair("Status:", if (monitorStatus == MonitorState.STARTED) "In use" else "Paused"),
        Pair("Recorded value:", "$twoDecimalValue ${if (variant == MonitorVariant.AUDIO) "dBFS" else "dBm"}"),
        Pair("Monitoring period:", monitorSettings.monitorPeriod.toString()),
        Pair("Counted measurements:", monitorSettings.measurementNumber.toString()),
        Pair("Local measurements:", measurementsNumber.toString()),
        Pair("Imported measurements:", externalMeasurementsNumber.toString()),
    )

    val roundedCornerShape: Shape = MaterialTheme.shapes.small
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.Gray, roundedCornerShape)
            .clip(roundedCornerShape)
    ) {
        infoLabels.keys.forEach { label ->
            Row(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .width(firstColumnLength)
                )

                Text(
                    text = infoLabels[label] ?: "",
                    color = Color.Gray,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}