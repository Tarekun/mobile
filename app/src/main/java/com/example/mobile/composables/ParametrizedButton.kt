package com.example.mobile.composables

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class ButtonVariant {
    FILLED,
    TONAL,
    OUTLINED,
    ELEVATED,
    TEXT
}

//TODO remove file
@Composable
fun ParametrizedButton(
    variant: ButtonVariant,
    onClick: () -> Unit,
    text: String,
    iconVector: ImageVector? = null,
    modifier: Modifier
): Unit {
    return when (variant) {
        ButtonVariant.FILLED ->
            Button(onClick = onClick) {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = "mobile"
                    )
                }
                Text(text = text)
            }
        ButtonVariant.TONAL ->
            FilledTonalButton(onClick = onClick) {
                Text(text = text)
            }
        ButtonVariant.OUTLINED ->
            OutlinedButton(onClick = onClick) {
                Text(text = text)
            }
        ButtonVariant.ELEVATED ->
            ElevatedButton(onClick = onClick, elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp
            )) {
                Text(text = text)
            }
        ButtonVariant.TEXT ->
            TextButton(onClick = onClick) {
                Text(text = text)
            }
    }
}