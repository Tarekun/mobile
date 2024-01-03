package com.example.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mobile.R
import com.example.mobile.composables.AlertSeverity
import com.example.mobile.composables.AlertTextbox
import com.example.mobile.composables.OptionSelect

@Composable
fun SettingLayout(
    title: String,
    description: String,
    inputField: @Composable() () -> Unit
) {
    var showDialog: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                showDialog = true
            }) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Setting explanation")
            }

            if (showDialog) {
                AlertDialog(
                    title = {
                        Text(text = title)
                    },
                    text = {
                        Text(text = description)
                    },
                    onDismissRequest = {
                        showDialog = false
                    },
                    confirmButton = { },
                    dismissButton = { }
                )

            }
        }
        inputField()
    }
}

@Composable
fun NumberSetting(
    title: String,
    description: String,
    value: Int,
    onChange: (newValue: Int) -> Unit,
    max: Int = Int.MAX_VALUE,
    min: Int = Int.MIN_VALUE
) {
    SettingLayout(
        title = title,
        description = description,
        inputField = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = value.toString(),
                    onValueChange = {
                        val newValue = it.toIntOrNull() ?: 0
                        onChange(newValue)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (value > min) {
                                    onChange(value - 1)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (value < max) {
                                    onChange(value + 1)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun <T> OptionsSetting(
    title: String,
    description: String,
    value: T,
    options: List<T>,
    onChange: (newValue: T) -> Unit,
    getLabel: (option: T) -> String = { it.toString() },
) {
    SettingLayout(
        title = title,
        description = description,
        inputField = {
            OptionSelect(
                label = "",
                options = options,
                onChange = {
                    onChange(it)
                },
                defaultOption = value,
                getLabel = getLabel,
                buttonModifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val gridSizes = listOf(10, 100, 1000)
    //TODO: initialize these values
    var monitoringPeriod: Int by remember { mutableStateOf(10) }
    var monitoringsNumber: Int by remember { mutableStateOf(10) }
    var gridUnitLength: Int by remember { mutableStateOf(10) }

    Column {
        AlertTextbox(
            severity = AlertSeverity.INFO,
            content = LocalContext.current.getString(R.string.settings_alert_note)
        )

        NumberSetting(
            title = context.getString(R.string.period_setting_title),
            description = context.getString(R.string.period_setting_desc),
            value = monitoringPeriod,
            onChange = {
                monitoringPeriod = it
                // store the update on db
            }
        )
        NumberSetting(
            title = context.getString(R.string.measurement_setting_title),
            description = context.getString(R.string.measurement_setting_desc),
            value = monitoringsNumber,
            onChange = {
                monitoringsNumber = it
                // store the update on db
            }
        )
        OptionsSetting(
            title = context.getString(R.string.grid_setting_title),
            description = context.getString(R.string.grid_setting_desc),
            value = gridUnitLength,
            onChange = {
                gridUnitLength = it
                // store the update on db
            },
            options = gridSizes
        )
    }
}