package com.example.mobile.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun <T> OptionSelect(
    label: String,
    options: List<T>,
    onChange: (selectedOption: T) -> Unit,
    getLabel: (option: T) -> String = { it.toString() },
    defaultOption: T? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(defaultOption) as MutableState<T> }

    Column() {
        OutlinedButton(
            onClick = { expanded = true },
        ) {
            Text(
                label + if(selectedOption != null) " (${getLabel(selectedOption)})" else ""
            )
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        selectedOption = option
                        expanded = false
                        onChange(selectedOption)
                    },
                    text = { Text(text = getLabel(option)) }
                )
            }
        }
    }
}