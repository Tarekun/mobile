package com.example.mobile.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.InputStream

const val dumpMimeType: String = "application/json"

@Composable
fun ExportScreen(
    variant: MonitorVariant,
    startIntent: (intent: Intent) -> Unit,
) {
    val context = LocalContext.current
    val spacing = Modifier.padding(bottom = 16.dp)

    val fileSelector = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { selectedUri: Uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    inputStream?.use {
                        val content = it.bufferedReader().use { reader -> reader.readText() }
                        MeasurementsUtils.storeExternalDump(content)
                    }
                }
            }
        }
    }

    fun export() {
        CoroutineScope(Dispatchers.IO).launch {
            val file: File = createTempFile("${variant.name}-dump", ".json")
            file.writeText(MeasurementsUtils.getJsonLocalCollection(variant))
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                context.getString(R.string.file_provider_authority),
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = dumpMimeType
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

            startIntent(shareIntent)
        }
    }

    fun importDump() {
        val fileSelectionIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = dumpMimeType
        }
        fileSelector.launch(fileSelectionIntent)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        OutlinedButton(
            onClick = {
                importDump()
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