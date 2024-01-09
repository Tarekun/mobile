package com.example.mobile.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ExportScreen() {


    fun export() {

    }

    Button(onClick = {
        export()
    }) {
        Text(text = "export your measurements")
    }
}