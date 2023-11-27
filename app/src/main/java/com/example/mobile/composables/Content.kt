package com.example.mobile.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun Content(
    currentVolume: Double,
    start: () -> Unit,
    stop: () -> Unit,
    onPeriodUpdate: (newPeriod: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val twoDecimalValue = (currentVolume * 100.0).roundToInt() / 100.0
    var periodMs by remember { mutableLongStateOf(1) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Slider(
                        value = periodMs.toFloat(),
                        steps = 1400 / 5,
                        onValueChange = { periodMs = it.roundToLong() },
                        valueRange = 1f..1440f,
                    )
                    Text(text = "$periodMs", modifier = modifier)
                }
                Button(onClick = { onPeriodUpdate(periodMs) }) {
                    Text(text = "Update period")
                }
            }
            
            Text(text = "Current Value: $twoDecimalValue dBFS/dBm", modifier = modifier)
            Button(onClick = start) {
                Text(text = "Start recorder")
            }
            Button(onClick = stop) {
                Text(text = "Stop recorder")
            }
        }
    }
}