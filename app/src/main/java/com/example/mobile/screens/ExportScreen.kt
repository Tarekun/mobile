package com.example.mobile.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.mobile.R
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.monitors.MonitorVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.File.createTempFile

@Composable
fun ExportScreen(
    variant: MonitorVariant,
    startIntent: (intent: Intent) -> Unit,
) {
    val context = LocalContext.current
    val spacing = Modifier.padding(bottom = 16.dp)

    fun export() {
        CoroutineScope(Dispatchers.IO).launch {
            val file: File = createTempFile("${variant.name}-dump", ".json")
            file.writeText(MeasurementsUtils.getJsonLocalCollection(variant))
            var fileUri = FileProvider.getUriForFile(
                context,
                context.getString(R.string.file_provider_authority),
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/json"
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

            startIntent(shareIntent)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        OutlinedButton(
            onClick = {
                //TODO
            },
            modifier = spacing
        ) {
            Text(text = context.getString(R.string.exportscreen_import_button))
        }
        OutlinedButton(
            onClick = {
                export()
            },
            modifier = spacing
        ) {
            Text(text = context.getString(R.string.exportscreen_export_button))
        }
        OutlinedButton(
            onClick = {
                //TODO
            },
            modifier = spacing
        ) {
            Text(text = context.getString(
                R.string.exportscreen_proximity_enabled
            ))
        }
    }

}