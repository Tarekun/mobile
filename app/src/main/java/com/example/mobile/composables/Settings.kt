package com.example.mobile.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp


@Composable
fun SettingLayout(
    title: String? = null,
    description: String = "",
    inputField: @Composable() () -> Unit
) {
    var showDialog: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        if (title != null) {
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
                if (description != "") {
                    IconButton(onClick = {
                        showDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Setting explanation"
                        )
                    }
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
        }
        inputField()
    }
}

@Composable
fun NumberSetting(
    title: String,
    description: String,
    value: Long,
    onChange: (newValue: Long) -> Unit,
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
                        val newValue = it.toLongOrNull() ?: 0
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
                value = value,
                getLabel = getLabel,
                buttonModifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun SwitchSetting(
    title: String? = null,
    label: String,
    value: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    SettingLayout(
        title = title,
        inputField = {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (value) Icons.Default.Check
                        else Icons.Filled.Clear,
                    contentDescription = contentDescription
                )
                Text(text = label)
            }
        }
    )
}