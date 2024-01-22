package com.example.mobile.screens

import android.app.Activity
import android.content.Context
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.monitors.MonitorVariant
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import java.io.FileNotFoundException

fun storePayload(payload: Payload, context: Context): Unit {
    val payloadFile: Uri? = payload.asFile()?.asUri()
    payloadFile?.let {
        CoroutineScope(Dispatchers.IO).launch {
            MeasurementsUtils.storeJsonDumpUri(
                context.contentResolver.openInputStream(it)
            )
        }
    }
}

fun sendCollectionAsPayload(endpointId: String, context: Context): Unit {
    CoroutineScope(Dispatchers.IO).launch {
        val fileToSend: File = File.createTempFile("dump", ".json")
        fileToSend.writeText(MeasurementsUtils.getJsonFullLocalCollection())
        val filePayload = Payload.fromFile(fileToSend)
        Nearby.getConnectionsClient(context)
            .sendPayload(endpointId, filePayload)
    }
}

fun makePayloadCallback(context: Context): PayloadCallback {
    return object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            storePayload(payload, context)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                //transfer successful??
            }
        }
    }
}
fun makeSendConnectionCallback(context: Context): ConnectionLifecycleCallback {
    return object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(context)
                .acceptConnection(endpointId, makePayloadCallback(context))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    sendCollectionAsPayload(endpointId, context)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> { }
                ConnectionsStatusCodes.STATUS_ERROR -> { }
            }
        }

        override fun onDisconnected(endpointId: String) { }
    }
}


@Composable
fun ProximityShareScreen(
    endpointId: String
) {
    val context = LocalContext.current
    val spacing = Modifier.padding(bottom = 16.dp)


    fun requestConnection(endpointId: String) {
        Nearby.getConnectionsClient(context)
            .requestConnection(
                "local name",
                endpointId,
                makeSendConnectionCallback(context)
            ).addOnSuccessListener {
                Log.d("mio", "successo request connection")
                // send notification
            }
            .addOnFailureListener {
                Log.d("mio", "fallimento request connection")
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        OutlinedButton(
            onClick = {
                requestConnection(endpointId)
            },
            modifier = spacing
        ) {
            Text(text = "Start connection to share database dumps with $endpointId")
        }
    }

}