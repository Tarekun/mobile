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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.mobile.R
import com.example.mobile.composables.SettingLayout
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.SettingsNames
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.MonitorVariant
import com.example.mobile.misc.NotificationHelper
import com.example.mobile.composables.SwitchSetting
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.File.createTempFile

const val dumpMimeType: String = "application/json"
const val serviceId: String = "proximityShare"
val strategy = Strategy.P2P_POINT_TO_POINT

@Composable
fun ExportScreen(
    variant: MonitorVariant,
    startIntent: (intent: Intent) -> Unit,
) {
    val context = LocalContext.current
    val spacing = Modifier.padding(bottom = 16.dp)

    var enableProximityShare: Boolean by remember { mutableStateOf(false) }

    val fileSelector = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { selectedUri: Uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    MeasurementsUtils.storeJsonDumpUri(
                        context.contentResolver.openInputStream(selectedUri)
                    )
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


    fun notifyUser(endpointId: String) {
        NotificationHelper.sendProximityNotification(
            context.getString(R.string.notification_title_endpoint_found),
            context.getString(R.string.notification_content_endpoint_found) + " " + endpointId,
            endpointId
        )
    }


    val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            notifyUser(endpointId)
        }

        override fun onEndpointLost(endpointId: String) { }
    }
    val sendConnectionCallback = makeSendConnectionCallback(context)

    //TODO: change the failureListener to proper handling
    fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions
            .Builder()
            .setStrategy(strategy)
            .build()

        Nearby.getConnectionsClient(context)
            .startAdvertising(
                // TODO: make this some kind of username?
                "local name",
                serviceId,
                sendConnectionCallback,
                advertisingOptions
            ).addOnFailureListener {
                Log.d("mio", "fallimento advertising: ${it.message}")
            }
    }
    //TODO: change the failureListener to proper handling
    fun startDiscovery() {
        val discoveryOptions: DiscoveryOptions = DiscoveryOptions
            .Builder()
            .setStrategy(strategy)
            .build()

        Nearby.getConnectionsClient(context)
            .startDiscovery(
                serviceId,
                discoveryCallback,
                discoveryOptions
            ).addOnFailureListener {
                Log.d("mio", "fallimento discovery")
            }
    }
    fun stopAdvertising() {
        Nearby.getConnectionsClient(context)
            .stopAdvertising()
    }
    fun stopDiscovery() {
        Nearby.getConnectionsClient(context)
            .stopDiscovery()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            enableProximityShare = SettingsUtils.storedSettings.enableProximityShare
        }
//        notifyUser("ENDPOINTID")
    }
    LaunchedEffect(enableProximityShare) {
        withContext(Dispatchers.IO) {
            SettingsUtils.updateSingleSetting(SettingsNames.ENABLE_PROXIMITY_SHARE, enableProximityShare.toString())
            // sharing was just enabled
            if (enableProximityShare) {
                startAdvertising()
                startDiscovery()
            }
            // otherwise sharing was just disabled
            else {
                stopAdvertising()
                stopDiscovery()
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        SettingLayout(
            inputField = {
                OutlinedButton(
                    onClick = {
                        importDump()
                    },
//                    modifier = spacing
                ) {
                    Text(text = context.getString(R.string.exportscreen_import_button))
                }
            }
        )
        SettingLayout(
            inputField = {
                OutlinedButton(
                    onClick = {
                        export()
                    },
//                    modifier = spacing
                ) {
                    Text(text = context.getString(R.string.exportscreen_export_button))
                }
            }
        )
        SwitchSetting(
            label = context.getString(
                if (enableProximityShare) R.string.exportscreen_proximity_enabled
                else R.string.exportscreen_proximity_disabled
            ),
            onClick = {
                enableProximityShare = !enableProximityShare
            },
            value = enableProximityShare,
            contentDescription = "Control proximity share functionality",
            modifier = spacing,
        )
    }

}