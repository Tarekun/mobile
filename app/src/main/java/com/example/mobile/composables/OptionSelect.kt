package com.example.mobile.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier

@Composable
fun <T> OptionSelect(
    label: String,
    options: List<T>,
    value: T?,
    onChange: (selectedOption: T) -> Unit,
    getLabel: (option: T) -> String = { it.toString() },
    defaultOption: T? = null,
    buttonModifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = value

    Column() {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = buttonModifier
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
//                        selectedOption = option
                        expanded = false
                        onChange(option)
                    },
                    text = { Text(text = getLabel(option)) }
                )
            }
        }
    }
}