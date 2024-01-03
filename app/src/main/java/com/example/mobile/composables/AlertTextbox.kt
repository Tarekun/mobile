package com.example.mobile.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AlertSeverity {
    INFO, SUCCESS, WARNING, ERROR
}

fun getColorForSeverity(severity: AlertSeverity) {
    when (severity) {
        AlertSeverity.INFO -> Color.Blue
        AlertSeverity.SUCCESS -> Color.Green
        AlertSeverity.WARNING -> Color.Yellow
        AlertSeverity.ERROR -> Color.Red
    }
}

@Composable
fun AlertTextbox(
    severity: AlertSeverity,
    title: String = "",
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
//            .background(getColorForSeverity(severity))
            .clip(MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))

            Column() {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = content,
                    color = Color.White
                )
            }
        }
    }
}