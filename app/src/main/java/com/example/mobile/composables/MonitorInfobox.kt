package com.example.mobile.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.monitors.MonitorState
import com.example.mobile.monitors.MonitorVariant

@Composable
fun MonitorInfobox(
    variant: MonitorVariant,
    monitorStatus: MonitorState,
    value: Double
) {
    val firstColumnLength = 200.dp
    //to avoid duplicate code
    val names = listOf(
        "Selected monitor:",
        "Status:",
        "Recorded value:"
    )
    val values = listOf(
        variant.name,
        if (monitorStatus == MonitorState.STARTED) "In use" else "Paused",
        "$value ${if (variant == MonitorVariant.AUDIO) "dBFS" else "dBm"}"
    )

    // Border color and shape
    val borderColor = Color.Gray
    val roundedCornerShape: Shape = MaterialTheme.shapes.small
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, borderColor, roundedCornerShape)
            .clip(roundedCornerShape)
    ) {
        names.forEachIndexed { index, infoName ->
            Row(modifier = Modifier
                .padding(8.dp)
            ) {
                Text(
                    text = infoName,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .width(firstColumnLength)
                )

                Text(
                    text = values[index],
                    color = Color.Gray,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}