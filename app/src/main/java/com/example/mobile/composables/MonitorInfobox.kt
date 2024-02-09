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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.R
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
    val context = LocalContext.current

    val twoDecimalValue = (currentValue * 100.0).roundToInt() / 100.0
    val firstColumnLength = 200.dp
    //to avoid duplicate code
    val infoLabels = mapOf<String, String>(
        Pair(context.getString(R.string.infobox_selected_monitor), variant.name),
        Pair(context.getString(R.string.infobox_status), if (monitorStatus == MonitorState.STARTED) context.getString(R.string.infobox_status_active) else context.getString(R.string.infobox_status_paused)),
        Pair(context.getString(R.string.infobox_recorded_value), "$twoDecimalValue ${if (variant == MonitorVariant.AUDIO) context.getString(R.string.dbfs_unit) else context.getString(R.string.dbm_unit)}"),
        Pair(context.getString(R.string.infobox_monitoring_period), monitorSettings.monitorPeriod.toString()),
        Pair(context.getString(R.string.infobox_counted_measurements), monitorSettings.measurementNumber.toString()),
        Pair(context.getString(R.string.infobox_local_measurements), measurementsNumber.toString()),
        Pair(context.getString(R.string.infobox_imported_measurements), externalMeasurementsNumber.toString()),
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